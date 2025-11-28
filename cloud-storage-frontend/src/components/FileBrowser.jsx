import { useEffect, useState } from "react";
import { listFolders } from "../api"; // твоя функция API (переименуй под свой бекенд)
import "./FileBrowser.css"; // стили ниже

export default function FileBrowser() {
  const [folders, setFolders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchData() {
      try {
        const data = await listFolders();
        setFolders(data);
      } catch (e) {
        console.error("Ошибка загрузки папок:", e);
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, []);

  return (
      <div className="fb-container">

        {/* Верх */}
        <div className="fb-top">
          <a className="fb-profile" href="/profile">Личный кабинет</a>
        </div>

        {/* Содержимое */}
        <div className="fb-content">
          {loading ? (
              <p className="fb-empty">Загрузка...</p>
          ) : folders.length === 0 ? (
              <p className="fb-empty">Здесь пусто(</p>
          ) : (
              <div className="fb-grid">
                {folders.map((folder) => (
                    <div key={folder.id} className="fb-folder">
                      📁 {folder.name}
                    </div>
                ))}
              </div>
          )}
        </div>

        {/* Нижние кнопки */}
        <div className="fb-bottom">
          <button className="fb-btn">Создать папку</button>
          <button className="fb-btn">Загрузить файл</button>
        </div>

      </div>
  );
}
