import { useEffect, useRef, type ComponentProps } from "react"
import { useForm, type UseFormRegister, type FieldErrors } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { checkoutSchema, type CheckoutFormValues } from "@/lib/validation/checkout"
import { checkoutErrorMessage } from "@/lib/api/checkout"
import { usePlaceOrder } from "@/hooks/usePlaceOrder"
import type { Address } from "@/types/address"
import type { Cart } from "@/types/cart"
import type { Order } from "@/types/order"
import type { Payment } from "@/types/payment"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card } from "@/components/ui/card"
import { Field } from "@/components/checkout/Field"
import { OrderSummary } from "@/components/checkout/OrderSummary"

interface CheckoutFormProps {
  cart: Cart
  email: string
  savedAddresses: Address[]
  onPlaced: (result: { order: Order; payment: Payment }) => void
}

export function CheckoutForm({ cart, email, savedAddresses, onPlaced }: CheckoutFormProps) {
  const placeOrder = usePlaceOrder(savedAddresses)
  const submitting = useRef(false)
  const didPrefill = useRef(false)
  const stockIssue = cart.items.find((item) => item.quantity > item.availableStock)

  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    formState: { errors },
  } = useForm<CheckoutFormValues>({
    resolver: zodResolver(checkoutSchema),
    mode: "onBlur",
    defaultValues: {
      fullName: "",
      email,
      mobile: "",
      line1: "",
      line2: "",
      city: "",
      state: "",
      country: "India",
      postalCode: "",
      paymentMethodId: "",
    },
  })

  useEffect(() => {
    if (!email || getValues("email")) return
    setValue("email", email)
  }, [email, getValues, setValue])

  useEffect(() => {
    if (didPrefill.current || savedAddresses.length === 0) return
    didPrefill.current = true
    const address = savedAddresses.find((item) => item.isDefault) ?? savedAddresses[0]
    setValue("line1", address.line1)
    setValue("line2", address.line2 ?? "")
    setValue("city", address.city)
    setValue("state", address.state)
    setValue("country", address.country)
    setValue("postalCode", address.postalCode)
  }, [savedAddresses, setValue])

  async function onSubmit(values: CheckoutFormValues) {
    if (submitting.current || stockIssue) return
    submitting.current = true
    try {
      const result = await placeOrder.mutateAsync(values)
      onPlaced(result)
    } catch {
      // The mutation stores the error. The message is rendered below.
    } finally {
      submitting.current = false
    }
  }

  const busy = placeOrder.isPending
  const serverError = placeOrder.error ? checkoutErrorMessage(placeOrder.error) : null

  return (
    <form
      id="checkout-form"
      onSubmit={handleSubmit(onSubmit)}
      className="mt-8 grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]"
      noValidate
    >
      <div className="order-2 space-y-6 lg:order-1">
      <Card className="space-y-4 p-5">
        <h2 className="font-display text-base font-semibold text-ink">Customer information</h2>
        <Field id="fullName" label="Full name" error={errors.fullName?.message}>
          <TextInput id="fullName" autoComplete="name" error={errors.fullName?.message} {...register("fullName")} />
        </Field>
        <Field id="email" label="Email" error={errors.email?.message}>
          <TextInput id="email" type="email" autoComplete="email" error={errors.email?.message} {...register("email")} />
        </Field>
        <Field id="mobile" label="Mobile number" error={errors.mobile?.message} hint="10 digits, starting with 6, 7, 8, or 9.">
          <TextInput id="mobile" type="tel" inputMode="numeric" autoComplete="tel" error={errors.mobile?.message} {...register("mobile")} />
        </Field>
        <p className="text-xs text-ink-muted">
          Name and mobile are checked on this page. The order service saves your shipping address and account email, not a separate phone number.
        </p>
      </Card>

      <Card className="space-y-4 p-5">
        <h2 className="font-display text-base font-semibold text-ink">Shipping address</h2>
        {savedAddresses.length > 0 && (
          <ul className="space-y-2">
            {savedAddresses.map((address) => (
              <li key={address.id}>
                <button
                  type="button"
                  onClick={() => {
                    setValue("line1", address.line1, { shouldValidate: true })
                    setValue("line2", address.line2 ?? "", { shouldValidate: true })
                    setValue("city", address.city, { shouldValidate: true })
                    setValue("state", address.state, { shouldValidate: true })
                    setValue("country", address.country, { shouldValidate: true })
                    setValue("postalCode", address.postalCode, { shouldValidate: true })
                  }}
                  className="w-full rounded-xl border border-border px-3 py-2 text-left text-sm text-ink transition-colors hover:border-brand/40 hover:bg-brand-dim/40"
                >
                  <span className="font-medium">{address.line1}</span>
                  <span className="mt-0.5 block text-xs text-ink-muted">
                    {[address.line2, address.city, address.state, address.postalCode, address.country]
                      .filter(Boolean)
                      .join(", ")}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
        <AddressFields register={register} errors={errors} />
      </Card>

      <Card className="space-y-4 p-5">
        <h2 className="font-display text-base font-semibold text-ink">Payment method</h2>
        <fieldset className="space-y-3">
          <legend className="sr-only">Payment method</legend>
          <label className="flex items-start gap-3 rounded-xl border border-brand/40 bg-brand-dim/40 px-3 py-3 text-sm">
            <input type="radio" name="payment-method" checked readOnly className="mt-1 accent-brand" />
            <span>
              <span className="font-medium text-ink">Card via Stripe</span>
              <span className="mt-0.5 block text-xs text-ink-muted">
                This is the only method the payment service accepts. Do not type a card number here.
              </span>
            </span>
          </label>
        </fieldset>
        <Field
          id="paymentMethodId"
          label="Stripe payment method id"
          error={errors.paymentMethodId?.message}
          hint="Starts with pm_. The charge runs only after you place the order."
        >
          <TextInput
            id="paymentMethodId"
            autoComplete="off"
            spellCheck={false}
            error={errors.paymentMethodId?.message}
            {...register("paymentMethodId")}
          />
        </Field>
      </Card>

      {stockIssue && (
        <p className="text-sm text-red-600">
          {stockIssue.name} only has {stockIssue.availableStock} in stock. Update the cart before placing the order.
        </p>
      )}
      {serverError && <p className="text-sm text-red-600">{serverError}</p>}

        <Button type="submit" variant="accent" size="lg" className="w-full lg:hidden" disabled={busy || Boolean(stockIssue)}>
          {busy ? "Placing order…" : "Place order"}
        </Button>
      </div>

      <aside className="order-1 lg:sticky lg:top-24 lg:order-2">
        <OrderSummary cart={cart} />
        <Button
          type="submit"
          variant="accent"
          size="lg"
          className="mt-4 hidden w-full lg:inline-flex"
          disabled={busy || Boolean(stockIssue)}
        >
          {busy ? "Placing order…" : "Place order"}
        </Button>
      </aside>
    </form>
  )
}

function AddressFields({
  register,
  errors,
}: {
  register: UseFormRegister<CheckoutFormValues>
  errors: FieldErrors<CheckoutFormValues>
}) {
  return (
    <div className="space-y-4">
      <Field id="line1" label="Address line 1" error={errors.line1?.message}>
        <TextInput id="line1" autoComplete="address-line1" error={errors.line1?.message} {...register("line1")} />
      </Field>
      <Field id="line2" label="Address line 2 (optional)" error={errors.line2?.message}>
        <TextInput id="line2" autoComplete="address-line2" error={errors.line2?.message} {...register("line2")} />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field id="city" label="City" error={errors.city?.message}>
          <TextInput id="city" autoComplete="address-level2" error={errors.city?.message} {...register("city")} />
        </Field>
        <Field id="state" label="State" error={errors.state?.message}>
          <TextInput id="state" autoComplete="address-level1" error={errors.state?.message} {...register("state")} />
        </Field>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field id="country" label="Country" error={errors.country?.message}>
          <TextInput id="country" autoComplete="country-name" error={errors.country?.message} {...register("country")} />
        </Field>
        <Field
          id="postalCode"
          label="PIN code"
          error={errors.postalCode?.message}
          hint="6 digits."
        >
          <TextInput
            id="postalCode"
            inputMode="numeric"
            autoComplete="postal-code"
            error={errors.postalCode?.message}
            {...register("postalCode")}
          />
        </Field>
      </div>
    </div>
  )
}

function TextInput({
  id,
  error,
  ...props
}: ComponentProps<typeof Input> & { error?: string }) {
  return (
    <Input
      {...props}
      id={id}
      aria-invalid={error ? true : undefined}
      aria-describedby={error ? `${id}-error` : undefined}
    />
  )
}
