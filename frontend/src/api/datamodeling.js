import api from './request'

export const dataDomainApi = {
  list: (params) => api.get('/datamodeling/domains', { params }),
  listAll: () => api.get('/datamodeling/domains/all'),
  get: (id) => api.get(`/datamodeling/domains/${id}`),
  create: (data) => api.post('/datamodeling/domains', data),
  update: (id, data) => api.put(`/datamodeling/domains/${id}`, data),
  delete: (id) => api.delete(`/datamodeling/domains/${id}`)
}

export const businessProcessApi = {
  list: (params) => api.get('/datamodeling/processes', { params }),
  get: (id) => api.get(`/datamodeling/processes/${id}`),
  create: (data) => api.post('/datamodeling/processes', data),
  update: (id, data) => api.put(`/datamodeling/processes/${id}`, data),
  delete: (id) => api.delete(`/datamodeling/processes/${id}`)
}

export const wordRootApi = {
  list: (params) => api.get('/datamodeling/wordroots', { params }),
  create: (data) => api.post('/datamodeling/wordroots', data),
  update: (id, data) => api.put(`/datamodeling/wordroots/${id}`, data),
  delete: (id) => api.delete(`/datamodeling/wordroots/${id}`)
}

export const modifierApi = {
  list: (params) => api.get('/datamodeling/modifiers', { params }),
  create: (data) => api.post('/datamodeling/modifiers', data),
  update: (id, data) => api.put(`/datamodeling/modifiers/${id}`, data),
  delete: (id) => api.delete(`/datamodeling/modifiers/${id}`)
}

export const timePeriodApi = {
  list: (params) => api.get('/datamodeling/timeperiods', { params }),
  create: (data) => api.post('/datamodeling/timeperiods', data),
  update: (id, data) => api.put(`/datamodeling/timeperiods/${id}`, data),
  delete: (id) => api.delete(`/datamodeling/timeperiods/${id}`)
}

export const dimensionApi = {
  list: (params) => api.get('/datamodeling/dimensions', { params }),
  get: (id) => api.get(`/datamodeling/dimensions/${id}`),
  create: (data) => api.post('/datamodeling/dimensions', data),
  update: (id, data) => api.put(`/datamodeling/dimensions/${id}`, data),
  saveFields: (id, fields) => api.post(`/datamodeling/dimensions/${id}/fields`, fields),
  generateDDL: (id) => api.post(`/datamodeling/dimensions/${id}/generate-ddl`),
  publish: (id, publishedBy) => api.post(`/datamodeling/dimensions/${id}/publish`, null, { params: { publishedBy } })
}

export const factTableApi = {
  list: (params) => api.get('/datamodeling/facttables', { params }),
  get: (id) => api.get(`/datamodeling/facttables/${id}`),
  create: (data) => api.post('/datamodeling/facttables', data),
  update: (id, data) => api.put(`/datamodeling/facttables/${id}`, data),
  saveFields: (id, fields) => api.post(`/datamodeling/facttables/${id}/fields`, fields),
  generateDDL: (id) => api.post(`/datamodeling/facttables/${id}/generate-ddl`),
  publish: (id, publishedBy) => api.post(`/datamodeling/facttables/${id}/publish`, null, { params: { publishedBy } })
}

export const summaryTableApi = {
  list: (params) => api.get('/datamodeling/summarytables', { params }),
  get: (id) => api.get(`/datamodeling/summarytables/${id}`),
  create: (data) => api.post('/datamodeling/summarytables', data),
  update: (id, data) => api.put(`/datamodeling/summarytables/${id}`, data),
  saveFields: (id, fields) => api.post(`/datamodeling/summarytables/${id}/fields`, fields),
  generateDDL: (id) => api.post(`/datamodeling/summarytables/${id}/generate-ddl`),
  publish: (id, publishedBy) => api.post(`/datamodeling/summarytables/${id}/publish`, null, { params: { publishedBy } })
}

export const publishHistoryApi = {
  list: (params) => api.get('/datamodeling/publish-history', { params })
}