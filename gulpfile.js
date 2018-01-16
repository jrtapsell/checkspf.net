const handlebarsStatic = require("gulp-compile-handlebars");
const gulp = require("gulp");
const rename = require("gulp-rename");
var path = require('path');
var handlebars = require('gulp-handlebars');
var wrap = require('gulp-wrap');
var declare = require('gulp-declare');
var concat = require('gulp-concat');
var merge = require('merge-stream');

gulp.task("pages", function () {
    return gulp.src("src/main/hbs/static/pages/*.hbs")
        .pipe(handlebarsStatic(null, {
            ignorePartials: false,
            batch: ["src/main/hbs/static/partials"],
            compile: {strict: true}/*,
            helpers: {
                "load_file": function (filename, options) {
                    var data = require("./src/data/" + filename);
                    return new handlebars.Handlebars.SafeString(options.fn(data));
                }
            }*/
        }))
        .pipe(rename({
            extname: ".html"
        }))
        .pipe(gulp.dest("web"));
});

gulp.task('templates', function() {
    // Assume all partials start with an underscore
    // You could also put them in a folder such as source/templates/partials/*.hbs
    var partials = gulp.src(['src/main/hbs/dynamic/partials/*.hbs'])
        .pipe(handlebars())
        .pipe(wrap('Handlebars.registerPartial(<%= processPartialName(file.relative) %>, Handlebars.template(<%= contents %>));', {}, {
            imports: {
                processPartialName: function(fileName) {
                    // Strip the extension and the underscore
                    // Escape the output with JSON.stringify
                    return JSON.stringify(path.basename(fileName, '.js').substr(1));
                }
            }
        }));

    var templates = gulp.src('src/main/hbs/dynamic/templates/*.hbs')
        .pipe(handlebars())
        .pipe(wrap('Handlebars.template(<%= contents %>)'))
        .pipe(declare({
            namespace: 'SPFViewer.templates',
            noRedeclare: true // Avoid duplicate declarations
        }));

    // Output both the partials and the templates as build/js/templates.js
    return merge(partials, templates)
        .pipe(concat('templates.js'))
        .pipe(gulp.dest('web/static/js/hbs/'));
});

gulp.task("build", gulp.series("pages","templates"));