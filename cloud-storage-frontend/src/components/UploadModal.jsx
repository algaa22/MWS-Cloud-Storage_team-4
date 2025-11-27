import React, { useRef, useState } from 'react';
import API from '../api.js';

export default function UploadModal({ token, path, onUploaded }) {
  const input = useRef();
  const [progress, setProgress] = useState(0);

  const upload = async (files) => {
    for (const file of files) {
      await API.upload(token, file, path);
    }
    setProgress(0);
    onUploaded();
  };

  return (
      <div className="p-3 border rounded bg-white">
        <button className="px-3 py-1 border rounded" onClick={()=>input.current.click()}>Загрузить</button>
        <input ref={input} type="file" className="hidden" onChange={(e)=>upload(e.target.files)} />
        {progress>0 && <div>Загрузка: {progress}%</div>}
      </div>
  );
}