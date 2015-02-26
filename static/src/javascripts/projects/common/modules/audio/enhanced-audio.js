define([
    'fastdom',
    'qwery'
], function (
    fastdom,
    qwery
) {
    function EnhancedAudio(player) {
        this.player = player;
        this.audioElement = qwery('audio', player.el())[0];
        this.audioContext = new (window.AudioContext || window.webkitAudioContext)();

        this.sourceNode = this.audioContext.createMediaElementSource(this.audioElement);
        this.analyser = this.audioContext.createAnalyser();
        this.analyser.minDecibels = -90;
        this.analyser.maxDecibels = -10;
        this.analyser.smoothingTimeConstant = 0.85;
        this.sourceNode.connect(this.analyser);
        this.analyser.connect(this.audioContext.destination);

        this.analyser.fftSize = 2048;

        this.bufferLength = this.analyser.frequencyBinCount;
        this.dataArray = new Uint8Array(this.bufferLength);
        this.analyser.getByteTimeDomainData(this.dataArray);

        var canvas = document.getElementById('mycanvas');
        this.canvasContext = canvas.getContext('2d');
        this.WIDTH = canvas.width;
        this.HEIGHT = canvas.height;

        this.draw();
    }

    EnhancedAudio.prototype.draw = function () {
        fastdom.defer(2, this.draw.bind(this));

        this.analyser.getByteTimeDomainData(this.dataArray);

        this.canvasContext.fillStyle = 'rgb(0, 0, 0)';
        this.canvasContext.fillRect(0, 0, this.WIDTH, this.HEIGHT);

        this.canvasContext.lineWidth = 3;
        this.canvasContext.strokeStyle = '#aad8f1';

        this.canvasContext.beginPath();

        var sliceWidth = this.WIDTH * 1.0 / this.bufferLength;
        var x = 0;

        for(var i = 0; i < this.bufferLength; i++) {

            var v = this.dataArray[i] / 128.0;
            var y = v * this.HEIGHT/2;

            if(i === 0) {
                this.canvasContext.moveTo(x, y);
            } else {
                this.canvasContext.lineTo(x, y);
            }

            x += sliceWidth;
        }

        this.canvasContext.lineTo(this.WIDTH, this.HEIGHT/2);
        this.canvasContext.stroke();
    };

    return EnhancedAudio;
});
