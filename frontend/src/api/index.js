import axios from 'axios'

axios.defaults.baseURL = ''
axios.defaults.withCredentials = true

axios.interceptors.request.use(async config => {
  const method = (config.method || 'get').toLowerCase()
  if (!['get', 'head', 'options'].includes(method)) {
    await axios.get('/auth/csrf')
  }
  return config
})

export default axios
