const vscode = require('vscode');
const cmds = require('./lib/main.js');

/**
 * @param {vscode.ExtensionContext} context
 */
function activate(context) {
	cmds.activate();
}
exports.activate = activate;

function deactivate() {
	cmds.deactivate()
}

module.exports = {
	activate,
	deactivate
}
