import request from '../utils/request'

export const getLocation = () => {
  return new Promise((resolve, reject) => {
    uni.getLocation({
      type: 'gcj02',
      success: resolve,
      fail: reject
    })
  })
}

export const reverseLocation = (lat, lng) => request.get('/location/reverse', { lat, lng })

export const geocodeLocation = (address) => request.get('/location/geocode', { address })