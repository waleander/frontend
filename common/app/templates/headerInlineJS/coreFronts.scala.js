@(item: model.MetaData)(implicit request: RequestHeader)

(function (navigator, window) {
    var isCoreFrontsMvtParticipant = function () {
        try {
            var participations = window.localStorage.getItem('gu.ab.participations');
            if (participations) {
                var jParts = JSON.parse(participations).value;
                return jParts["IpadCoreFronts"].variant === 'core';
            }
            return false;
        } catch (e) {
            return false;
        }
    };

    window.coreFronts = @item.isFront && isCoreFrontsMvtParticipant();

})(navigator, window);
