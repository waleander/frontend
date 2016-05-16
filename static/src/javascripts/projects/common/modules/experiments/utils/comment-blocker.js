define(
    ['lodash/arrays/reduce'],
function(reduce) {

    function showComments(shortUrl) {
       var sUrlInt = reduce(shortUrl.split(''), function(sum, ch) {
            return sum + ch.charCodeAt(0);
       }, 0);

       console.log("+++ Code for "  + shortUrl + " " + sUrlInt)
       return sUrlInt % 2 == 0;
    }

    return {
        showComments: showComments
    };
});
