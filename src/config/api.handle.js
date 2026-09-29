import instance from "./axios.customize";

export const callUploadImage = (file, folder) => {
    const bodyFormData = new FormData();
    bodyFormData.append('file', file);
    bodyFormData.append('folder', folder);
    return instance({
        method: 'post',
        url: '/api/v1/files',
        data: bodyFormData,
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
}
export const uploadVideoAPI = (file, folder) => {
    const bodyFormData = new FormData();
    bodyFormData.append('file', file);
    bodyFormData.append('folder', folder);
    return instance({
        method: 'post',
        url: '/api/v1/upload/video',
        data: bodyFormData,
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
}
// Episode
export const createEpisodeAPI = (title, filePath, contentType, seasonId) => {
    return instance.post("/api/v1/add-episode", {
        title, filePath,
        contentType,
        season: {
            "id": seasonId
        }
    })
}
export const fetchAllEpisodeBySeason = (seasonId) => {
    return instance.get(`/api/v1/episodes/by-season/${seasonId}`)
}
// Xử lý lại HLS cho 1 tập (dùng khi hlsStatus=FAILED hoặc tập cũ chưa có HLS trên R2).
export const processEpisodeAPI = (id) => {
    return instance.post(`/api/v1/process-episode/${id}`)
}
// Film
// Reference data (genres + highlight tags): lấy 1 lần với size lớn để Select không
// bị thiếu lựa chọn khi số thể loại/tag vượt quá page size mặc định (20).
export const fetchFilmCategory = (query = 'page=0&size=200') => {
    return instance.get(`/api/v1/categories?${query}`);
}
export const fetchFilmTags = (query = 'page=0&size=200') => {
    return instance.get(`/api/v1/tags?${query}`);
}
export const callCreateFilmAPI = (thumbnail, slider, name, studio, tags, categories) => {
    return instance.post('/api/v1/add-film', { thumbnail, slider, name, studio, tags, categories })
}
export const callUpdateFilmAPI = (id, thumbnail, slider, name, studio, tags, categories) => {
    return instance.put(`/api/v1/update-film`, { id, thumbnail, slider, name, studio, tags, categories })
}
export const fetchDataFilmsAPI = (query) => {
    return instance.get(`/api/v1/films?${query}`);
}
export const fetchFilmByIdAPI = (id) => {
    return instance.get(`/api/v1/film/${id}`)
}
//user
export const callUpdateAccountUserAPI = (id,) => {

}
// authentication
export const registerAPI = (fullName, email, password, roleId) => {
    return instance.post(`/api/v1/auth/register`, {
        fullName, email, password,
        role: {
            "id": roleId
        }
    })
}
export const loginAPI = (username, password) => {
    return instance.post(`/api/v1/auth/login`, { username, password })
}
export const callFetchAccountAPI = () => {
    return instance.get(`/api/v1/auth/account`)
}
export const LogoutAPI = () => {
    return instance.post('/api/v1/auth/logout')
}
// season of film
export const fetchSeasonsOfFilmAPI = (id, query) => {
    return instance.get(`/api/v1/seasons/by-film/${id}?${query}`)
}
export const AddSeasonAPI = (thumb, seasonName, description, type, releaseYear, status, trailer, ordinal, filmId) => {
    return instance.post(`/api/v1/add-season`,
        { thumb, seasonName, description, type, releaseYear, status, trailer, ordinal, film: { "id": filmId } })
}
export const UpdateSeasonAPI = (thumb, seasonName, description, type, releaseYear, status, trailer, ordinal, id) => {
    return instance.put(`/api/v1/update-season`,
        { thumb, seasonName, description, type, releaseYear, status, trailer, ordinal, id })
}

export const fetchAllSeasons = (query) => {
    return instance.get(`/api/v1/seasons?${query}`)
}
export const fetchSeasonById = (id) => {
    return instance.get(`/api/v1/season/${id}`)
}
export const fetchRelatedSeasonsAPI = (seasonId) => {
    return instance.get(`/api/v1/seasons/related/${seasonId}`)
}
export const checkView = (videoId) => {
    return instance.post(`/api/v1/${videoId}/view`)
}
export const callDeleteSeasonAPI = (id) => {
    return instance.delete(`/api/v1/delete-season/${id}`)
}
export const fetchTopHighViewAPI = () => {
    return instance.get(`/api/v1/seasons/top-views`)
}
export const fetchRatingAPI = (userId, seasonId, stars) => {
    return instance.post(`/api/v1/ratings?userId=${userId}&seasonId=${seasonId}&stars=${stars}`)
}
export const fetchAveragePointAPI = (seasonId) => {
    return instance.get(`/api/v1/ratings/average/${seasonId}`)
}
// comment & notifi
export const fecthCommentsAPI = (seasonId) => {
    return instance.get(`/api/v1/comments/${seasonId}`)
}
export const postCommentAPI = (seasonId, newContent, userId) => {
    return instance.post(`/api/v1/comments/post/${seasonId}`, { content: newContent, user: { "id": userId } })
}
export const replyCommentAPI = (seasonId, parentId, content) => {
    return instance.post(`/api/v1/comments/${seasonId}/${parentId}/reply`, { content })
}
export const reactCommentAPI = (commentId) => {
    return instance.post(`/api/v1/comments/${commentId}/like`)
}
export const getNotification = (userId) => {
    return instance.get(`/api/v1/notifications/user/${userId}`)
}
export const markAsReadNotifi = (id) => {
    return instance.put(`/api/v1/notifications/${id}/read`)
}
