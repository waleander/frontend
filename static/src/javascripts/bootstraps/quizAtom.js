define([
    'react',
    'common/utils/_',
    'common/utils/ajax',
    'common/utils/config',
    'common/views/svgs',
], function (
    React,
    _,
    ajax,
    config,
    svgs
) {

    function init (callback) {

        var ENDPOINT = '/atoms/quizzes/' + _.first(config.page.atomIds) + '.json';

        /**
         * Quiz - React class for interactive enhancement of a quiz atom
         *
         * This react class handles all interactivity for quizzes including
         * tracking a users score, displaying reveal text against correctly
         * chosen answers and giving an option to social share the relevant
         * result group text once the user has completed the quiz.
         *
         * The class requests the structured quiz data from the onward platform at startup.
         */
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

            /**
             * Selecting an Answer should:
             *  - Lock out the question from further changes and apply relevant styling
             *  - Display the correct answer and reveal text if the chosen answer was incorrect
             *  - Update the quiz score
             * @param questionIndex
             * @param answerIndex
             * @param event
             */
            selectAnswer: function (questionIndex, answerIndex, event) {

                var questions = this.state.quiz.content.questions;
                var thisQuestion = questions[questionIndex];

                if (typeof thisQuestion.checkedAnswer === 'undefined') {

                    var thisAnswer = thisQuestion.answers[answerIndex];

                    thisQuestion.checkedAnswer = answerIndex;

                    if (thisAnswer.correct) {
                        questions[questionIndex].markedCorrect = true;
                    } else { // Display correct answer
                        questions[questionIndex].markedCorrect = false;
                        questions[questionIndex].revealCorrect = true;
                    }

                    this.setState({
                        score: questions.reduce(function (p, c) {
                            return p + (c.markedCorrect ? 1 : 0);
                        }, 0)
                    });
                }
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
                    { className: 'u-responsive-ratio quiz-question__image' },
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
                                return React.createElement(Question, {
                                    question: question,
                                    questionIndex: questionIndex,
                                    renderImage: self.renderImage,
                                    selectAnswer: self.selectAnswer,
                                    key: questionIndex,
                                    quiz: self.state.quiz
                                });
                            })
                        ),
                        React.createElement(
                            'h3',
                            { className: 'quiz__score' },
                            'Score: ',
                            self.state.score,
                            '/',
                            self.state.quiz.content.questions.length
                        ),
                        React.createElement(ShareResult, {
                            score: self.state.score,
                            quiz: self.state.quiz
                        })
                    );
                }
            }
        });

        var ShareResult = React.createClass({

            enhanceWithScoreAndTitle: function (group) {
                var shareRegex = /([\s\S]+)_\/_([\s\S]+)<quiz title>/g;
                var replaceText = "$1" + this.props.score + "/" + this.props.quiz.content.questions.length + "$2" + this.props.quiz.title;
                group.enhancedShare = group.share.replace(shareRegex, replaceText);
                return group;
            },

            findResultGroup: function (score) {
                var quizContent = this.props.quiz.content;
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

            buildTweetUrl: function (tweetText) {
                // https://twitter.com/intent/tweet?text=Hello%20world
                var BASE_URL = 'https://twitter.com/intent/tweet?text=';
                var encodedTweet = encodeURIComponent(tweetText);
                return BASE_URL + encodedTweet;
            },

            renderTweetButton: function () {
                var scriptElement,
                    nativeTweetElements = qwery('blockquote.twitter-tweet'),
                    widgetScript = qwery('#twitter-widget');

                if (nativeTweetElements.length > 0) {
                    if (widgetScript.length === 0) {
                        scriptElement = document.createElement('script');
                        scriptElement.id = 'twitter-widget';
                        scriptElement.async = true;
                        scriptElement.src = '//platform.twitter.com/widgets.js';
                        $(document.body).append(scriptElement);
                    }

                    if (typeof twttr !== 'undefined' && 'widgets' in twttr && 'load' in twttr.widgets) {
                        twttr.widgets.load(body);
                    }
                }
            },

            render: function () {
                var self = this;
                var group = self.findResultGroup(self.props.score);
                if (group) {
                    return React.createElement(
                        'div',
                        { className: 'quiz__result-group' },
                        React.createElement(
                            'div',
                            { className: 'result-group' },
                            group.title,
                            ' | ',
                            group.enhancedShare
                        ),
                        React.createElement(
                            'a',
                            {
                                className: 'twitter-share-button',
                                href: self.buildTweetUrl(group.enhancedShare),
                                target: '_blank'
                            },
                            'Tweet'
                        )
                    );
                } else {
                    return React.createElement(
                        'div',
                        { className: 'quiz__result-group' },
                        'Complete the quiz to share your results...'
                    )
                }
            }
        });

        /**
         * Question - a React class representing a generic quiz question
         */
        var Question = React.createClass({

            getInitialState: function () {
                return {
                    quiz: null
                };
            },

            componentWillMount: function () {
                this.renderImage = this.props.renderImage;
                this.selectAnswer = this.props.selectAnswer;
                this.setState({
                    quiz: this.props.quiz
                });
            },

            componentWillReceiveProps: function (nextProps) {
                this.setState({
                    quiz: nextProps.quiz
                });
            },

            questionClass: function (question) {
                var BASE_QUESTION_CLASS = 'quiz__question question';
                return typeof question.checkedAnswer === 'undefined' ? BASE_QUESTION_CLASS : BASE_QUESTION_CLASS + ' quiz__question--locked';
            },

            render: function () {
                var self = this;

                var question = this.props.question;
                var questionIndex = this.props.questionIndex;

                var answers = question.answers || [];

                return React.createElement(
                    'li',
                    { className: this.questionClass(question) },
                    React.createElement(
                        'h4',
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
                            return React.createElement(Answer, {
                                question: question,
                                questionIndex: questionIndex,
                                revealCorrect: question.revealCorrect,
                                answer: answer,
                                answerIndex: answerIndex,
                                renderImage: self.renderImage,
                                selectAnswer: self.selectAnswer,
                                key: answerIndex,
                                quiz: self.state.quiz
                            })
                        })
                    )
                );
            }
        });

        /**
         * Answer - a React class representing a generic quiz answer option
         */
        var Answer = React.createClass({

            getInitialState: function () {
                return {
                    quiz: null
                };
            },

            componentWillMount: function () {
                this.renderImage = this.props.renderImage;
                this.selectAnswer = this.props.selectAnswer;
                this.setState({
                    quiz: this.props.quiz
                });
            },

            componentWillReceiveProps: function (nextProps) {
               this.setState({
                   quiz: nextProps.quiz
               });
            },

            answerClass: function (answer, questionIndex, answerIndex) {
                var BASE = 'question__answer answer',
                    CORRECT = 'answer--correct',
                    CORRECT_ACTIVE = 'is-active',
                    INCORRECT = 'answer--incorrect',
                    REVEALED = 'answer--revealed',
                    SPACE = ' ';
                var questions = this.state.quiz.content.questions;
                var thisQuestion = questions[questionIndex];
                var answerHasBeenClicked = thisQuestion.checkedAnswer === answerIndex;

                if (answerHasBeenClicked && answer.correct) {
                    return [BASE, SPACE, CORRECT, SPACE, CORRECT_ACTIVE].join('');
                } else if (answerHasBeenClicked && !answer.correct) {
                    return [BASE, SPACE, INCORRECT].join('');
                } else if (!answerHasBeenClicked && answer.correct && this.props.revealCorrect) { // Reveal correct answer if incorrect has been selected
                    return [BASE, SPACE, CORRECT, SPACE, REVEALED].join('');
                }

                return BASE;
            },

            renderRevealText: function (question, answer) {
                if (answer.correct && question.markedCorrect || answer.correct &&  this.props.revealCorrect) {
                    return React.createElement(
                        'span',
                        { className: 'answer__reveal-text' },
                        answer.revealText
                    );
                }
            },

            getRadioName: function (q, a) {
                return 'q' + q + 'a' + a;
            },

            renderCrossIcon: function () {
                return React.createElement(
                    'span',
                    {
                        className: 'quiz__answer-icon',
                        dangerouslySetInnerHTML: {
                            __html: svgs('quizIncorrect')
                        }
                    }, null);
            },

            renderTickIcon: function () {
                return React.createElement(
                    'span',
                    {
                        className: 'quiz__answer-icon',
                        dangerouslySetInnerHTML: {
                            __html: svgs('quizCorrect')
                        }
                    }, null);
            },

            renderAnswerIcon: function (question, answer, questionIndex, answerIndex) {
                if (typeof question.checkedAnswer !== 'undefined') {
                    if (answer.correct && question.markedCorrect || answer.correct &&  this.props.revealCorrect) { // Tick Icon
                        return this.renderTickIcon();
                    } else if (question.checkedAnswer === answerIndex && !answer.correct) { // Cross icon
                        return this.renderCrossIcon();
                    }
                }
            },

            render: function () {
                var self = this;

                var question = this.props.question;
                var questionIndex = this.props.questionIndex;

                var answer = this.props.answer;
                var answerIndex = this.props.answerIndex;

                var radioId = self.getRadioName(questionIndex, answerIndex);

                return React.createElement(
                    'li',
                    null,
                    React.createElement(
                        'label',
                        {
                            htmlFor: radioId,
                            className: self.answerClass(answer, questionIndex, answerIndex)
                        },
                        self.renderAnswerIcon(question, answer, questionIndex, answerIndex),
                        React.createElement(
                            'input',
                            {
                                type: 'radio',
                                id: radioId,
                                className: 'answer__radio',
                                value: answerIndex,
                                onChange: self.selectAnswer.bind(null, questionIndex, answerIndex)
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
