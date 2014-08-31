var compileSass = require('broccoli-ruby-sass');

var inputTrees = ['styles', 'bower_components'];
var inputFile = 'main.scss';
var outputFile = 'app.css';
var options = {};

var outputTree = compileSass(inputTrees, inputFile, outputFile, options);

module.exports = outputTree;
