import { useEffect } from "react";
/**
 * SuccessToast Component
 * @param {string} message - The success message to display
 * @param {function} onDismiss - Callback to clear the message
 * @param {number} duration - How long to show the toast (ms)
 */
const SuccessToast = ({ message, onDismiss, duration = 5000 }) => {
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(() => {
      onDismiss();
    }, duration);
    return () => clearTimeout(timer);
  }, [message, onDismiss, duration]);

  if (!message) return null;

  return (
    <div className="success-toast">
      <div className="success-content">
        <span className="success-message">{message}</span>
        <div
          className="success-progress"
          style={{ animationDuration: `${duration}ms` }}
        />
      </div>
    </div>
  );
};

export default SuccessToast;
