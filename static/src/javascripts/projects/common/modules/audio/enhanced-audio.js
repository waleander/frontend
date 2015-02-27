define([
    'bean',
    'fastdom',
    'qwery',
    'common/utils/_',
    'common/utils/$',
    'common/utils/ajax-promise',
    'common/utils/config',
    'common/utils/template'
], function (
    bean,
    fastdom,
    qwery,
    _,
    $,
    ajaxPromise,
    config,
    template
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

        this.visuals = 'bars';

        // wave
        //this.analyser.fftSize = 2048;
        //this.bufferLength = this.analyser.frequencyBinCount;
        //this.dataArray = new Uint8Array(this.bufferLength);

        // bars
        this.analyser.fftSize = 256;
        this.bufferLength = this.analyser.frequencyBinCount;
        this.dataArray = new Uint8Array(this.bufferLength);


        var canvas = document.getElementById('mycanvas');
        this.canvasContext = canvas.getContext('2d');
        this.WIDTH = canvas.width;
        this.HEIGHT = canvas.height;

        bean.on(document.body, 'click', '#mycanvas', this.onCanvasClick.bind(this));

        this.createPlaylist();
        this.draw();
    }

    EnhancedAudio.prototype.onCanvasClick = function () {
        switch (this.visuals) {
            case 'bars':
                this.visuals = 'wave';
                break;
            case 'wave':
                this.visuals = 'nothing';
                break;
            default:
                this.visuals = 'bars';
        }

        this.setVisuals(this.visuals);
    };

    EnhancedAudio.prototype.setVisuals = function (visuals) {
        this.visuals = visuals;

        switch (this.visuals) {
            case 'bars':
                this.analyser.fftSize = 256;
                this.bufferLength = this.analyser.frequencyBinCount;
                this.dataArray = new Uint8Array(this.bufferLength);
                break;
            case 'wave':
                this.analyser.fftSize = 2048;
                this.bufferLength = this.analyser.frequencyBinCount;
                this.dataArray = new Uint8Array(this.bufferLength);
                break;
        }

    };

    EnhancedAudio.prototype.createPlaylist = function () {
        //var capiTmpl = 'http://content.guardianapis.com/search?api-key={{key}}gnm-hackday&page-size=6&tag={{seriesId}}',


        if (config.page.seriesId) {

            var request = {
                url: 'http://content.guardianapis.com/search',
                type: 'json',
                method: 'get',
                crossOrigin: true,
                timeout: 5000,
                data: {
                    'api-key': 'gnm-hackday',
                    'page-size': 6,
                    tag: config.page.seriesId
                }
            };

            ajaxPromise(request).then(function (data) {
                var playlistItems = _.filter(data.response.results, function(result){ return result.id != config.page.pageId}),

                    $audioPlaylist = $.create('<div class="audio-player-playlist"></div>'),
                    playlistPosition = 1;

                _.forEach(playlistItems, function(playlistItem){
                    var webTitle = playlistItem.webTitle.split(' – ')[0].split(' - ')[0];
                    var $newItem = $.create(template('<div class="audio-player-playlist--item"><span class="audio-player-playlist--item-position">{{position}}</span> <a class="audio-player-playlist--item-title" data-src="">{{title}}</a></div>',
                    {
                        position: playlistPosition,
                        title: webTitle
                    }));
                    $audioPlaylist.append($newItem);
                    playlistPosition++;
                });

                $('.js-audio-playlist').append($audioPlaylist);

                console.log(playlistItems);
            });

        }

    };

    EnhancedAudio.prototype.draw = function () {
        fastdom.defer(2, this.draw.bind(this));

        if (this.visuals === 'wave') {


            this.analyser.getByteTimeDomainData(this.dataArray);

            this.canvasContext.fillStyle = 'rgb(0, 0, 0)';
            this.canvasContext.fillRect(0, 0, this.WIDTH, this.HEIGHT);

            this.canvasContext.lineWidth = 3;
            this.canvasContext.strokeStyle = '#aad8f1';

            this.canvasContext.beginPath();

            var sliceWidth = this.WIDTH * 1.0 / this.bufferLength;
            var x = 0;

            for (var i = 0; i < this.bufferLength; i++) {

                var v = this.dataArray[i] / 128.0;
                var y = v * this.HEIGHT / 2;

                if (i === 0) {
                    this.canvasContext.moveTo(x, y);
                } else {
                    this.canvasContext.lineTo(x, y);
                }

                x += sliceWidth;
            }

            this.canvasContext.lineTo(this.WIDTH, this.HEIGHT / 2);
            this.canvasContext.stroke();
        } else if (this.visuals === 'bars') {

            //this.analyser.fftSize = 256;
            //this.bufferLength = this.analyser.frequencyBinCount;

            //var dataArray = new Uint8Array(this.bufferLength);


            this.analyser.getByteFrequencyData(this.dataArray);

            this.canvasContext.fillStyle = 'rgb(0, 0, 0)';
            this.canvasContext.fillRect(0, 0, this.WIDTH, this.HEIGHT);

            var barWidth = (this.WIDTH / this.bufferLength) * 2.5;
            var barHeight;
            var x = 0;

            for(var i = 0; i < this.bufferLength; i++) {
                barHeight = this.dataArray[i];

                this.canvasContext.fillStyle = 'rgb(170,' + (barHeight+100) + ',270)';
                this.canvasContext.fillRect(x,this.HEIGHT-barHeight/2,barWidth,barHeight/2);

                x += barWidth + 1;
            }

        } else {
            this.canvasContext.fillStyle = 'rgb(0, 0, 0)';
        }
    };

    return EnhancedAudio;
});
