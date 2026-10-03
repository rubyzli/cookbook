// Placeholder text for loading, empty and error states
export default function StatusMessage({ children, role }) {
  return (
    <div className="status-message" role={role}>
      {children}
    </div>
  )
}
