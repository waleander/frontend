define([
    'react',
    'common/utils/_',
    'common/utils/ajax',
    'common/utils/config'
], function (
    React,
    _,
    ajax,
    config
) {

    function init (callback) {

        var ENDPOINT = '/atoms/quizzes/' + _.first(config.page.atomIds) + '.json';

        var Quiz = React.createClass({

            componentDidMount: function () {
                var self = this;
                ajax({
                    url: ENDPOINT,
                    type: 'json',
                    method: 'get',
                    crossOrigin: true
                }).then(function (resp) {
                    if (self.isMounted()) {
                        self.setState({
                            'score': 0,
                            'quiz': resp.assets.data
                        });
                    }
                });
            },

            getInitialState: function () {
                return {
                    'score': 0,
                    'quiz': null
                };
            },

            enhanceWithScoreAndTitle: function (group) {
                var shareRegex = /([\s\S]+)_\/_([\s\S]+)<quiz title>/g;
                var replaceText = "$1" + this.state.score + "/" + this.state.quiz.content.questions.length + "$2" + this.state.quiz.title;
                group.enhancedShare = group.share.replace(shareRegex, replaceText);
                return group;
            },

            findResultGroup: function (score) {
                var quizContent = this.state.quiz.content;
                var allQuestionsAnswered = _.every(quizContent.questions, function (question) {
                    return typeof question.checkedAnswer !== 'undefined';
                });
                if (allQuestionsAnswered) {
                    var groups = quizContent.resultGroups.groups;
                    var theGroup = _.findLast(groups, function (group, index) {
                        return group.minScore <= score;
                    });
                    if (theGroup) {
                        return this.enhanceWithScoreAndTitle(theGroup);
                    }
                }
                return false;
            },

            getRadioName: function (q, a) {
                return 'q' + q + 'a' + a;
            },

            updateScore: function (questionIndex, answerIndex, event) {
                var questions = this.state.quiz.content.questions;
                var thisQuestion = questions[questionIndex];
                var thisAnswer = thisQuestion.answers[answerIndex];

                thisQuestion.checkedAnswer = answerIndex;

                questions[questionIndex].markedCorrect = thisAnswer.correct ? true : false;

                this.setState({
                    score: questions.reduce(function (p, c) {
                        return p + (c.markedCorrect ? 1 : 0);
                    }, 0)
                });
            },

            responsiveImage: function (image) { // See img.scala.html

                var sizes = image.data.assets;

                var sortedBySize = _.sortBy(sizes, 'fields.width'); // smallest first

                function generateSrcSet (sortedBySize) { // <url> <width>w, <url> <width>w, etc...
                    var srcSet = [];
                    sortedBySize.forEach(function (img) {
                        var srcSetString = '';
                        srcSetString += img.secureUrl;
                        srcSetString += ' ';
                        srcSetString += img.fields.width + 'w';
                        srcSet.push(srcSetString);
                    });
                    return srcSet.join(', ');
                }

                return React.createElement('div',
                    { className: 'u-responsive-ratio preview-question__image' },
                    React.createElement('img',
                        {
                            className: 'gu-image',
                            src: _.last(sortedBySize).secureUrl,
                            srcSet: generateSrcSet(sortedBySize),
                            sizes: '(min-width: 660px) 620px, (min-width: 480px) 605px, 445px',
                            itemProp: 'contentUrl',
                            alt: image.data.fields.altText
                        })
                );
            },

            renderImage: function (assets) {
                var image = _.first(assets); // TODO: Validate, image may not be the first asset

                if (image && image.type === 'image') {
                    return this.responsiveImage(image);
                }
            },

            renderRevealText: function (question, answer) {
                if (answer.correct && question.markedCorrect) {
                    return React.createElement(
                        'span',
                        { className: 'answer__reveal-text' },
                        answer.revealText
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
                        group.enhancedShare
                    );
                }
            },

            answerClass: function (answer, questionIndex, answerIndex) {
                var BASE = 'question__answer answer',
                    CORRECT = 'answer--correct',
                    INCORRECT = 'answer--incorrect',
                    SPACE = ' ';
                var questions = this.state.quiz.content.questions;
                var thisQuestion = questions[questionIndex];
                var answerHasBeenClicked = thisQuestion.checkedAnswer === answerIndex;

                if (answerHasBeenClicked && answer.correct) {
                    return [BASE, SPACE, CORRECT].join('');
                } else if (answerHasBeenClicked && !answer.correct) {
                    return [BASE, SPACE, INCORRECT].join('');
                }

                return BASE;
            },

            render: function () {

                var self = this;

                if (!this.state.quiz) {
                    return React.createElement(
                        'div',
                        null,
                        'Quiz Not Found'
                    );
                } else {
                    var questions = this.state.quiz.content.questions || [];

                    return React.createElement(
                        'article',
                        { className: 'quiz' },
                        React.createElement(
                            'h2',
                            { className: 'quiz__title' },
                            self.state.quiz.title
                        ),
                        React.createElement(
                            'ol',
                            { className: 'quiz__questions' },
                            questions.map(function (question, questionIndex) {

                                var questionName = 'q' + questionIndex;

                                var answers = question.answers || [];

                                return React.createElement(
                                    'li',
                                    { className: 'quiz__question question', key: questionIndex },
                                    React.createElement(
                                        'p',
                                        { className: 'question__text' },
                                        self.renderImage(question.assets),
                                        React.createElement(
                                            'span',
                                            { className: 'question__number' },
                                            ['Q', (questionIndex + 1), ' '].join('')
                                        ),
                                        question.questionText
                                    ),
                                    React.createElement(
                                        'ol',
                                        { className: 'question__answers' },
                                        answers.map(function (answer, answerIndex) {
                                            var radioId = self.getRadioName(questionIndex, answerIndex);
                                            return React.createElement(
                                                'li',
                                                { key: answerIndex },
                                                React.createElement(
                                                    'label',
                                                    {
                                                        htmlFor: radioId,
                                                        className: self.answerClass(answer, questionIndex, answerIndex)
                                                    },
                                                    React.createElement(
                                                        'input',
                                                        {
                                                            type: 'radio',
                                                            id: radioId,
                                                            className: 'answer__radio',
                                                            name: questionName,
                                                            value: answerIndex,
                                                            onChange: self.updateScore.bind(self, questionIndex, answerIndex)
                                                        }
                                                    ),
                                                    self.renderImage(answer.assets),
                                                    React.createElement(
                                                        'span',
                                                        { className: 'answer__text' },
                                                        answer.answerText
                                                    ),
                                                    self.renderRevealText(question, answer)
                                                )
                                            );
                                        })
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
                    );
                }
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
