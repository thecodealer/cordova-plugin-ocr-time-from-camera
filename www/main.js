
function OcrTimeFromCamera() {}

OcrTimeFromCamera.prototype.callbacks = {};
OcrTimeFromCamera.prototype.serviceId = cordova.platformId == 'ios' ? 'Main' : 'Main';
OcrTimeFromCamera.prototype.on = function(name, callback) {
    this.callbacks[name] = callback;
}

OcrTimeFromCamera.prototype.emit = function(name, params) {
console.log('>>>>>>>...', name, params);
    if (this.callbacks.hasOwnProperty(name)) {
        this.callbacks[name](params);
    }
}

OcrTimeFromCamera.prototype.start = function() {
    cordova.exec(OcrTimeFromCameraSuccessCallback, OcrTimeFromCameraErrorCallback, this.serviceId, 'start', null);
}

OcrTimeFromCamera.prototype.stop = function() {
    cordova.exec(OcrTimeFromCameraSuccessCallback, OcrTimeFromCameraErrorCallback, this.serviceId, 'stop', null);
}

function OcrTimeFromCameraSuccessCallback(params) {
    if (params && params.event) {
        var eventData;
        if (params.data) {
            if (params.data.indexOf('[') !== -1 || params.data.indexOf('{') !== -1) {
                eventData = JSON.parse(params.data);
            }
            else {
                eventData = params.data;
            }
        }
        OcrTimeFromCameraInstance.emit(params.event, eventData);
    }
}

function OcrTimeFromCameraErrorCallback(error) {
    OcrTimeFromCameraInstance.emit('error', error);
}

window.OcrTimeFromCameraInstance = new OcrTimeFromCamera();

module.exports = OcrTimeFromCameraInstance;