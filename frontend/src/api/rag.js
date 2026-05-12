/**
 * RAG问答相关API
 */
import request from '../utils/request'

const BASE_URL = request.BASE_URL || 'http://localhost:8080/api'

export const ragAsk = (question, sessionId) => request.post('/rag/ask', { question, sessionId })

export const getSessionList = () => request.get('/rag/sessions')

export const getSessionHistory = (sessionId) => request.get(`/rag/sessions/${sessionId}/history`)

export const createSession = (title) => request.post('/rag/sessions', { title })

export const deleteSession = (sessionId) => request.del(`/rag/sessions/${sessionId}`)

export const renameSession = (sessionId, title) => request.put(`/rag/sessions/${sessionId}`, { title })

export const ragSearch = (params) => request.get('/rag/search', params)

export const addKnowledge = (data) => request.post('/rag/knowledge', data)

export const initKnowledgeBase = () => request.post('/rag/init')

export const ragAskAsync = (question, sessionId) => request.post('/rag/ask-async', { question, sessionId })

export const ragAskWithSkill = (question, sessionId) => request.post('/rag/ask-skill', { question, sessionId })

export const getTaskStatus = (taskId) => request.get(`/rag/task/${taskId}/status`)

export const getTaskResult = (taskId) => request.get(`/rag/task/${taskId}/result`)

export const ragAskStream = (question, sessionId, callbacks) => {
    const token = uni.getStorageSync('token') || ''
    const url = BASE_URL + `/rag/ask-stream?question=${encodeURIComponent(question)}&sessionId=${sessionId || ''}`
    const decoder = new TextDecoder('utf-8')

    let pendingLine = ''

    const processChunks = (text) => {
        pendingLine += text
        while (true) {
            const idx = pendingLine.indexOf('\n')
            if (idx === -1) break
            const line = pendingLine.substring(0, idx).trim()
            pendingLine = pendingLine.substring(idx + 1)
            if (line.startsWith('data:')) {
                const data = line.substring(5).trim().replace(/"/g, '')
                if (data && data !== '[DONE]') {
                    callbacks.onChunk && callbacks.onChunk(data)
                }
            }
        }
    }

    const requestTask = uni.request({
        url: url,
        method: 'GET',
        enableChunked: true,
        timeout: 60000,
        header: {
            'Authorization': token ? `Bearer ${token}` : '',
            'Content-Type': 'application/json'
        },
        success: (res) => {
            const leftover = decoder.decode()
            processChunks(leftover)
            callbacks.onDone && callbacks.onDone()
        },
        fail: (err) => {
            callbacks.onError && callbacks.onError(err)
        }
    })

    requestTask.onChunkReceived((res) => {
        const uint8Array = new Uint8Array(res.data)
        const text = decoder.decode(uint8Array, { stream: true })
        processChunks(text)
    })

    return requestTask
}

export const ragAskStreamWithFile = (question, sessionId, file, callbacks) => {
    const token = uni.getStorageSync('token') || ''
    const url = BASE_URL + `/rag/ask-stream?question=${encodeURIComponent(question)}&sessionId=${sessionId || ''}`
    const decoder = new TextDecoder('utf-8')

    let pendingLine = ''

    const processChunks = (text) => {
        pendingLine += text
        while (true) {
            const idx = pendingLine.indexOf('\n')
            if (idx === -1) break
            const line = pendingLine.substring(0, idx).trim()
            pendingLine = pendingLine.substring(idx + 1)
            if (line.startsWith('data:')) {
                const data = line.substring(5).trim().replace(/"/g, '')
                if (data && data !== '[DONE]') {
                    callbacks.onChunk && callbacks.onChunk(data)
                }
            }
        }
    }

    const requestTask = uni.uploadFile({
        url: url,
        filePath: file.path,
        name: 'file',
        header: {
            'Authorization': token ? `Bearer ${token}` : ''
        },
        success: (res) => {
            const leftover = decoder.decode()
            processChunks(leftover)
            callbacks.onDone && callbacks.onDone()
        },
        fail: (err) => {
            callbacks.onError && callbacks.onError(err)
        }
    })

    return requestTask
}
