import { useState } from "react";

/**
 * A reusable confirmation dialog component
 * @param {boolean} isOpen - Whether the dialog is visible
 * @param {string} title - Title of the dialog
 * @param {string} message - Message content
 * @param {function} onConfirm - Action to run when confirmed
 * @param {function} onCancel - Action to run when canceled
 */
const ConfirmDialog = ({ isOpen, title, message, onConfirm, onCancel }) => {
  if (!isOpen) return null;

  return (
    <div className="confirm-overlay">
      <div className="confirm-box">
        <h3>{title}</h3>
        <p>{message}</p>
        <div className="confirm-actions">
          <button className="cancel" onClick={onCancel}>
            Cancel
          </button>
          <button className="confirm" onClick={onConfirm}>
            Confirm
          </button>
        </div>
      </div>
    </div>
  );
};

/**
 * Custom hook for managing a confirmation dialog
 * @returns {object} - Contains ConfirmDialog component and control methods
 */
export const useConfirmDialog = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [options, setOptions] = useState({
    title: "",
    message: "",
    onConfirm: () => {},
  });

  /**
   * Shows the confirmation dialog
   * @param {string} title - Dialog title
   * @param {string} message - Dialog message
   * @param {function} onConfirm - Callback when confirmed
   */
  const showConfirm = (title, message, onConfirm) => {
    setOptions({ title, message, onConfirm });
    setIsOpen(true);
  };

  /** Handles cancel */
  const handleCancel = () => setIsOpen(false);

  /** Handles confirm */
  const handleConfirm = () => {
    options.onConfirm();
    setIsOpen(false);
  };

  return {
    ConfirmDialog: () => (
      <ConfirmDialog
        isOpen={isOpen}
        title={options.title}
        message={options.message}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    ),
    showConfirm,
  };
};

export default ConfirmDialog;
