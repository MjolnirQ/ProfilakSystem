import React, { useEffect, useState } from 'react'
import axios from '../../api'
import { getRequestDocuments } from '../../api/requests'
import '../../styles/DocumentViewer.css'

export default function DocumentViewer({ requestId, onClose }) {
  const [docs, setDocs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getRequestDocuments(requestId)
      .then(res => setDocs(res.data))
      .catch(() => setError('Не удалось загрузить документы'))
      .finally(() => setLoading(false))
  }, [requestId])

  async function downloadDocument(doc) {
    try {
      const response = await axios.get(`/staff/documents/${doc.id}`, { responseType: 'blob' })
      const url = URL.createObjectURL(response.data)
      const link = document.createElement('a')
      link.href = url
      link.download = doc.fileName
      link.click()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch {
      setError('Не удалось скачать документ')
    }
  }

  return (
    <div className="modal-backdrop">
      <div className="modal doc-modal">
        <h3>Документы заявки #{requestId}</h3>

        {loading && <p>Загрузка...</p>}
        {error && <p className="error">{error}</p>}

        {!loading && !error && docs.length === 0 && (
          <p>Документы не найдены</p>
        )}

        {!loading && docs.length > 0 && (
          <ul className="download-list">
            {docs.map(doc => (
              <li key={doc.id} className="download-item">
                <span className="file-name">{doc.fileName}</span>
                <button
                  type="button"
                  onClick={() => downloadDocument(doc)}
                  className="download-btn"
                >
                  Скачать
                </button>
              </li>
            ))}
          </ul>
        )}

        <button onClick={onClose} className="close-btn">Закрыть</button>
      </div>
    </div>
  )
}
