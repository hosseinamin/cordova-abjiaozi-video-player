var exec = require('cordova/exec');


module.exports = {
  startFullscreen: function (url, title, opts) {
    return new Promise(function (resolve, reject) {
      exec(resolve, reject, "abjiaoziVideoPlayer", "startFullscreen", [url, title, opts]);
    });
  },
};
