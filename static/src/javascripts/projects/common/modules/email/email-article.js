define([
    'common/utils/$',
    'bean',
    'bonzo',
    'common/modules/identity/api',
    'fastdom',
    'common/modules/email/email',
    'common/utils/detect',
    'lodash/collections/contains',
    'lodash/arrays/intersection',
    'common/utils/config',
    'lodash/collections/every',
    'lodash/collections/find',
    'text!common/views/email/iframe.html',
    'common/utils/template'
], function (
    $,
    bean,
    bonzo,
    Id,
    fastdom,
    email,
    detect,
    contains,
    intersection,
    config,
    every,
    find,
    iframeTemplate,
    template
) {

    var listConfigs = {
            theCampaignMinute: {
                canRun: 'theCampaignMinute',
                modClass: 'post-article',
                insertMethod: 'insertAfter',
                insertSelector: '.js-article__container'
            },
            theGuardianToday: {
                canRun: 'theGuardianToday'
            },
            profileellehunt: {
                canRun: 'profileellehunt'
            }
        },
        emailInserted = false,
        addListToPage = function (listConfig) {
            if (listConfig) {
                var iframe = bonzo.create(template(iframeTemplate, listConfig))[0],
                    $iframeEl = $(iframe),
                    $insertEl = $(listConfig.insertSelector);

                bean.on(iframe, 'load', function () {
                    email.init(iframe);
                });

                fastdom.write(function () {
                    $iframeEl[listConfig.insertMethod || 'appendTo']($insertEl && $insertEl.length > 0 ? $insertEl : $('.js-article__body'));
                });

                emailInserted = true;
            }
        },
        emailFormExists = function () {
            return false;
        },
        canRunList = {
            theGuardianToday: function () {
                var host = window.location.host,
                    escapedHost = host.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, '\\$&'), // Escape anything that will mess up the regex
                    urlRegex = new RegExp('^https?:\/\/' + escapedHost + '\/(uk\/|us\/|au\/|international\/)?([a-z-])+$', 'gi');

                return urlRegex.test(document.referrer) && !Id.isUserLoggedIn();
            },
            profileellehunt: function () {
                return true;
            }
        };

    return {
        init: function () {
            // THIS IS WEIRD.
            var tagId = config.page.emailSignUp,
                escapedTagId = config.page.emailSignUp.replace(/[.,\/#!$%\^&\*;:{}=\-_`~()]/g, '');

            if (tagId && !emailFormExists() && !emailInserted) {
                if (!listConfigs[escapedTagId] || canRunList[escapedTagId]()) {
                    addListToPage({tagId: tagId});
                }
            }
        }
    };
});
