define([
    'react',
    'common/utils/_'
], function (
    React,
    _
) {

    function init (callback) {
        var Quiz = React.createClass({

            getInitialState: function () {
                return {
                    'score': 0,
                    'quiz': guardian.config.page.atoms[0]
                };
            },

            findResultGroup: function (score) {
                var groups = this.state.quiz.content.resultGroups.groups;
                return _.findLast(groups, function (group, index) {
                    return group.minScore <= score;
                });
            },

            getRadioName: function (q, a) {
                return 'q' + q + 'a' + a;
            },

            updateScore: function (questionIndex, answerIndex, event) {
                var questions = this.state.quiz.content.questions;
                var thisQuestion = questions[questionIndex];
                var thisAnswer = thisQuestion.answers[answerIndex];

                thisQuestion.checkedAnswer = answerIndex;

                if (thisAnswer.correct) {
                    questions[questionIndex].markedCorrect = true;
                } else {
                    questions[questionIndex].markedCorrect = false;
                }

                this.setState({
                    score: questions.reduce(function (p, c) {
                        return p + (c.markedCorrect ? 1 : 0);
                    }, 0)
                });
            },

            renderQuestionImage: function (question) {
                var asset = _.first(question.assets);

                if (asset && asset.type === 'image') {
                    return React.createElement(
                        'div',
                        { className: 'preview-question__image' },
                        React.createElement('img', { width: '300', src: asset.data.quizBuilderDisplayUrl })
                    );
                }
            },

            renderResultGroup: function (group) {
                if (group) {
                    return React.createElement(
                        'div',
                        { className: 'result-group' },
                        group.title,
                        ' | ',
                        group.share
                    );
                }
            },

            renderAnswerIcon: function (answer, questionIndex, answerIndex) {

                var questions = this.state.quiz.content.questions;
                var thisQuestion = questions[questionIndex];
                var answerHasBeenClicked = thisQuestion.checkedAnswer === answerIndex;

                if (answerHasBeenClicked && answer.correct) {
                    return React.createElement('i', { className: 'i-tick-green' });
                } else if (answerHasBeenClicked && !answer.correct) {
                    return React.createElement('i', { className: 'i-cross-red' });
                }
            },

            renderQuestionImage: function (question) {
                var asset = _.first(question.assets);

                if (asset && asset.type === 'image') {
                    return React.createElement(
                        'div',
                        { className: 'preview-question__image' },
                        React.createElement('img', { width: '300', src: asset.data.quizBuilderDisplayUrl })
                    );
                }
            },

            renderResultGroup: function (group) {
                if (group) {
                    return React.createElement(
                        'div',
                        { className: 'result-group' },
                        group.title,
                        ' | ',
                        group.share
                    );
                }
            },

            renderAnswerIcon: function (answer, questionIndex, answerIndex) {

                var questions = this.state.quiz.content.questions;
                var thisQuestion = questions[questionIndex];
                var answerHasBeenClicked = thisQuestion.checkedAnswer === answerIndex;

                if (answerHasBeenClicked && answer.correct) {
                    return React.createElement('i', { className: 'i-tick-green' });
                } else if (answerHasBeenClicked && !answer.correct) {
                    return React.createElement('i', { className: 'i-cross-red' });
                }
            },

            render: function () {

                var _this2 = this;

                var self = this;

                var questions = this.state.quiz.content.questions || [];

                if (!this.state.quiz) {
                    return React.createElement(
                        'div',
                        null,
                        'Quiz Not Found'
                    );
                }

                return React.createElement(
                    'article',
                    { className: 'editor__main' },
                    React.createElement(
                        'div',
                        { className: 'editor__question-scroll-cont' },
                        React.createElement(
                            'div',
                            { className: 'preview__panel' },
                            React.createElement(
                                'h2',
                                { className: 'preview__title' },
                                self.state.quiz.title
                            ),
                            React.createElement(
                                'ul',
                                null,
                                questions.map(function (question, questionIndex) {

                                    var questionName = 'q' + questionIndex;

                                    var answers = question.answers || [];

                                    return React.createElement(
                                        'li',
                                        { className: 'preview-question' },
                                        React.createElement(
                                            'div',
                                            { className: 'preview-question__number' },
                                            'Q',
                                            questionIndex + 1
                                        ),
                                        React.createElement(
                                            'div',
                                            { className: 'preview-question__question' },
                                            React.createElement(
                                                'span',
                                                { className: 'preview-question__text' },
                                                question.questionText
                                            ),
                                            self.renderQuestionImage(question),
                                            React.createElement(
                                                'div',
                                                { className: 'preview-question__answers' },
                                                answers.map(function (answer, answerIndex) {
                                                    var radioId = self.getRadioName(questionIndex, answerIndex);
                                                    return React.createElement(
                                                        'div',
                                                        null,
                                                        React.createElement(
                                                            'label',
                                                            { htmlFor: radioId, className: 'preview-question__answer' },
                                                            React.createElement(
                                                                'span',
                                                                { className: 'preview__correct' },
                                                                _this2.renderAnswerIcon(answer, questionIndex, answerIndex)
                                                            ),
                                                            React.createElement('input', {
                                                                type: 'radio',
                                                                id: radioId,
                                                                name: questionName,
                                                                value: answerIndex,
                                                                onChange: self.updateScore.bind(self, questionIndex, answerIndex) }),
                                                            answer.answerText
                                                        )
                                                    );
                                                })
                                            )
                                        )
                                    );
                                })
                            ),
                            React.createElement(
                                'div',
                                { className: 'preview__score' },
                                'Score: ',
                                self.state.score,
                                '/',
                                self.state.quiz.content.questions.length
                            ),
                            React.createElement(
                                'div',
                                { className: 'preview__result-group' },
                                self.renderResultGroup(self.findResultGroup(self.state.score))
                            )
                        )
                    )
                );

            }
        });

        React.render(
            React.createElement(Quiz),
            document.getElementsByTagName('gu-atom')[0],
            callback
        );
    }


    var module = {
        DOM_ID: 'js-quiz-atom',
        init: init
    };
    return module;
});
