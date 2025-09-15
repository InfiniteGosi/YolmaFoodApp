import { useState } from "react";
import { loadStripe } from "@stripe/stripe-js";
import {
  Elements,
  CardElement,
  useStripe,
  useElements,
} from "@stripe/react-stripe-js";
import ApiService from "../../services/ApiService";
import { useError } from "../common/ErrorDisplay";

const stripeInstance = loadStripe(process.env.REACT_APP_STRIPE_PUBLIC_KEY);

const PaymentForm = ({ amount, orderId, onSuccess }) => {
  const stripe = useStripe();
  const elements = useElements();

  const [loading, setLoading] = useState(false);
  const { ErrorDisplay, showError } = useError();

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!stripe || !elements) {
      return;
    }

    setLoading(true);

    try {
      // Initialized payment
      const body = {
        amount: amount,
        orderId: orderId,
      };

      const paymentInitializeResponse = await ApiService.proceedForPayment(
        body
      );
      if (paymentInitializeResponse.statusCode !== 200) {
        throw new Error(
          paymentInitializeResponse.message || "Failed ot initialize payment"
        );
      }

      const uniqueTransactionId = paymentInitializeResponse.data;

      // Confirm payment with stripe

      const { error: stripeError, paymentIntent } =
        await stripe.confirmCardPayment(uniqueTransactionId, {
          payment_method: {
            card: elements.getElement(CardElement),
            billing_details: {
              // Add any additional billing details you want
            },
          },
        });

      if (stripeError) {
        throw stripeError;
      }

      if (paymentIntent.status === "succeeded") {
        const res = await ApiService.updateOrderPayment({
          orderId,
          amount,
          transactionId: paymentIntent.id,
          success: true,
        });

        onSuccess(paymentIntent);
      } else {
        const res = await ApiService.updateOrderPayment({
          orderId,
          amount,
          transactionId: paymentIntent.id,
          success: false,
        });
      }
    } catch (exception) {
      showError(exception.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="payment-form">
      <ErrorDisplay />
      <div className="form-group">
        <CardElement />
      </div>

      <button
        type="submit"
        disabled={!stripe || loading}
        className="pay-button"
      >
        {loading ? "Processing..." : `Pay $${amount}`}
      </button>
    </form>
  );
};

const Payment = ({ amount, orderId, onSuccess }) => {
  return (
    <div className="payment-container">
      <h2>Complete Payment</h2>

      <Elements stripe={stripeInstance}>
        <PaymentForm amount={amount} orderId={orderId} onSuccess={onSuccess} />
      </Elements>
    </div>
  );
};

export default Payment;
