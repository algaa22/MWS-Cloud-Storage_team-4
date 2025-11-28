export default function FileItem({ file }) {
  return (
      <div className="bg-blue-800/40 p-4 rounded-2xl border border-blue-500/20 text-white">
        <p>{file.path}</p>
      </div>
  );
}