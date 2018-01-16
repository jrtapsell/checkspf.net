const handlebars = require("gulp-compile-handlebars");
const gulp = require("gulp");
const rename = require("gulp-rename");

var handlebars2 = require('gulp-handlebars');
var wrap = require('gulp-wrap');
var declare = require('gulp-declare');
var concat = require('gulp-concat');

gulp.task("pages", function () {
    return gulp.src("src/main/hbs/static/pages/*.hbs")
        .pipe(handlebars(null, {
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

gulp.task('templates', function(){
    return gulp.src('src/main/hbs/dynamic/templates/*.hbs')
        .pipe(handlebars2())
        .pipe(wrap('Handlebars.template(<%= contents %>)'))
        .pipe(declare({
            namespace: 'SPFViewer.templates',
            noRedeclare: true, // Avoid duplicate declarations
        }))
        .pipe(concat('templates.js'))
        .pipe(gulp.dest('web'));
});

gulp.task("build", gulp.series("pages", "templates"));