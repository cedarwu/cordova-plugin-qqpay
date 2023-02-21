#!/usr/bin/env node

module.exports = function (context) {
    var path        = require('path'),
        fs          = require('fs'),
        shell       = require('shelljs'),
        projectRoot = context.opts.projectRoot,
        plugins     = context.opts.plugins || [];

    // The plugins array will be empty during platform add
    if (plugins.length > 0 && plugins.indexOf('cordova-plugin-qqpay') === -1) {
        console.info("cordova-plugin-qqpay not found");
        return ;
    }

    var ConfigParser = null;
    try {
        ConfigParser = context.requireCordovaModule('cordova-common').ConfigParser;
    } catch(e) {
        // fallback
        ConfigParser = context.requireCordovaModule('cordova-lib/src/configparser/ConfigParser');
    }

    var config      = new ConfigParser(path.join(context.opts.projectRoot, "config.xml")),
        packageName = config.android_packageName() || config.packageName();

    // replace dash (-) with underscore (_)
    packageName = packageName.replace(/-/g , "_");

    console.info("Running android-install.Hook: " + context.hook + ", Package: " + packageName + ", Path: " + projectRoot + ".");

    if (!packageName) {
        console.error("Package name could not be found!");
        return ;
    }

    // android platform available?
    if (context.opts.cordova.platforms.indexOf("android") === -1) {
        console.info("Android platform has not been added.");
        return ;
    }

    var targetDir = path.join(projectRoot, "platforms", "android", "app", "src", "main", "java", packageName.replace(/\./g, path.sep), "qqpay");

    var targetFiles = ["CallbackActivity.java"];

    if (['after_plugin_add', 'after_plugin_install'].indexOf(context.hook) === -1) {
        // remove it?
        targetFiles.forEach(function (f) {
            try {
                fs.unlinkSync(path.join(targetDir, f));
            } catch (err) {}
        });
    } else {
        // create directory
        shell.mkdir('-p', targetDir);

        // sync the content
        targetFiles.forEach(function (f) {
            fs.readFile(path.join(context.opts.plugin.dir, 'src', 'android', f), {encoding: 'utf-8'}, function (err, data) {
                if (err) {
                    throw err;
                }

                data = data.replace(/^package __PACKAGE_NAME__;/m, 'package ' + packageName + '.qqpay;');
                fs.writeFileSync(path.join(targetDir, f), data);
                console.info("write file:", path.join(targetDir, f));
            });
        });
    }
};
