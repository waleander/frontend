define([
    'bean',
    'common/utils/$',
    'common/utils/fastdom-promise',
    'lodash/collections/toArray'
], function (
    bean,
    $,
    fastdom,
    toArray
) {

    var modules = {
        // find a bucket message to show once you finish a quiz
        handleCompletion: function () {
            // we're only handling completion in browsers who can validate forms natively
            // others do a round trip to the server
            if (HTMLFormElement.prototype.checkValidity) {
                // quizzes can be set to only show answers at the end, in which case we do a round trip.
                // we'll run this code only if it's an instant-reveal quiz
                var $quizzes = $('.js-atom-quiz--instant-reveal'),
                    $numberOfQuestions = $('.js-atom__quiz_question').length;

                if ($quizzes.length > 0) {
                    bean.on(document, 'click', toArray($quizzes), function (e) {
                        var quiz = e.currentTarget,
                            total = $(':checked + .atom-quiz__answer__item--is-correct', quiz).length;

                        console.log("+++ Click : " + total + " Of: " + $numberOfQuestions);
                        modules.recordQuizProgressUpdate(total, $numberOfQuestions)
                        if (quiz.checkValidity()) { // the form (quiz) is complete
                            console.log("+++ Click complete " + total);
                            var $bucket__message = null;
                            do {
                                // try and find a .bucket__message for your total
                                $bucket__message = $('.js-atom-quiz__bucket-message--' + total, quiz);

                                // if we find a message for your total show it, and exit
                                if ($bucket__message.length > 0) {
                                    fastdom.write(function () {
                                        $bucket__message.css({
                                            'display': 'block'
                                        });
                                    });
                                    break;
                                }

                                // if we haven't exited, there's no .bucket__message for your score, so you must be in
                                // a bucket with a range that begins below your total score
                                total--;
                            } while (total >= 0); // the lowest we'll look is for 0 correct answers
                        }
                    });
                }
                else {
                    var $quizzes = $('.js-atom-quiz');
                    console.log("++ The Others Ones: " + $quizzes.length);
                    if ($quizzes.length > 0) {
                        bean.on(document, 'click', toArray($quizzes), function(e) {
                            var quiz = e.currentTarget,
                                total = $(':checked + .atom-quiz__answer__item', quiz).length
                            console.log("+++ Click : " + total + " Of: " + $numberOfQuestions);
                            modules.recordQuizProgressUpdate(total, $numberOfQuestions);
                        });
                        bean.on($quizzes[0], 'click', '.js-atom-quiz--submit', function(){
                            console.log("++ Completo");
                        })
                    }
                }
            }
        },

        recordQuizProgressUpdate: function(totalQuestions, questionsAnswered) {
            var data = {};
            data['QuizProgressUpdate'] = {
                questions: totalQuestions,
                answered: questionsAnswered
            }
            modules.ophanRecord(data)
       },


        ophanRecord: function(data) {
            require('ophan/ng', function (ophan) {
                ophan.record(data);
           });
        }
    };


    return {
        handleCompletion: modules.handleCompletion
    };
});
