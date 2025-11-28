import React from 'react';
import API from '../api.js';

export default function PreviewModal({ item, token, onClose }) {
  if (!item) return null;

  return (
      <div className="fixed inset-0 bg-black/40 flex items-center justify-center" onClick={onClose}>
        <div className="bg-white p-4 rounded w-2/3 max-h-[80vh] overflow-auto" onClick={(e)=>e.stopPropagation()}>
          <div className="flex justify-between mb-3">
            <strong>{item.name}</strong>
            <button onClick={onClose}>Закрыть</button>
          </div>

          <img src={API.downloadUrl(token, item.path)} className="max-h-[70vh] mx-auto" />
        </div>
      </div>
  );
}