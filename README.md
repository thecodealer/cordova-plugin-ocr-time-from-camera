# cordova-plugin-ocr-time-from-camera

## Usage
### Here's how you'd use it in the sample cordova app
#### index.html
```
<div class="app">
    <h1>Apache Cordova</h1>
    <div id="deviceready" class="blink">
        <p class="event listening">Connecting to Device</p>
        <p class="event received">Device is Ready</p>
    </div>
    <br>
    <button class="btn-start" disabled>Start</button>
</div>
```
#### index.js
```
var app = {
    // Application Constructor
    initialize: function() {
        document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);
    },

    // deviceready Event Handler
    //
    // Bind any cordova events here. Common events are:
    // 'pause', 'resume', etc.
    onDeviceReady: function() {
        this.receivedEvent('deviceready');
    },

    // Update DOM on a Received Event
    receivedEvent: function(id) {
        var parentElement = document.getElementById(id);
        var listeningElement = parentElement.querySelector('.listening');
        var receivedElement = parentElement.querySelector('.received');

        listeningElement.setAttribute('style', 'display:none;');
        receivedElement.setAttribute('style', 'display:block;');

        console.log('Received Event: ' + id);

        var elBtnStart = document.querySelector('.btn-start');
        elBtnStart.removeAttribute('disabled');
        elBtnStart.addEventListener('click', function() {
            cordova.plugins.OcrTimeFromCamera.start();
        });

        cordova.plugins.OcrTimeFromCamera.on('started', function() {
            console.log('started');
        });

        cordova.plugins.OcrTimeFromCamera.on('stopped', function() {
            console.log('stopped');
        });

        cordova.plugins.OcrTimeFromCamera.on('captured', function(time) {
           console.log('captured', data);
           alert('time: ' + time);
        });

        cordova.plugins.OcrTimeFromCamera.on('error', function(error) {
           console.log('error', error);
        });
    }
};

app.initialize();
```