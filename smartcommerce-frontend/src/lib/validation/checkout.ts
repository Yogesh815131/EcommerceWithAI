import { z } from "zod"

export const checkoutSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(2, "Enter your full name")
    .max(80, "Name must be 80 characters or fewer"),
  email: z.string().trim().email("Enter a valid email").max(255, "Email is too long"),
  mobile: z
    .string()
    .trim()
    .regex(/^[6-9]\d{9}$/, "Enter a 10-digit mobile number starting with 6–9"),
  line1: z
    .string()
    .trim()
    .min(5, "Enter your street address")
    .max(200, "Address line 1 is too long"),
  line2: z.string().trim().max(200, "Address line 2 is too long"),
  city: z.string().trim().min(2, "Enter your city").max(80, "City name is too long"),
  state: z.string().trim().min(2, "Enter your state").max(80, "State name is too long"),
  country: z.string().trim().min(2, "Enter your country").max(80, "Country name is too long"),
  postalCode: z.string().trim().regex(/^\d{6}$/, "Enter a 6-digit PIN code"),
  paymentMethodId: z
    .string()
    .trim()
    .regex(/^pm_[A-Za-z0-9]+$/, "Enter a Stripe payment method id that starts with pm_"),
})

export type CheckoutFormValues = z.infer<typeof checkoutSchema>
