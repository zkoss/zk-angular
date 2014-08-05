(function (angular) {
// for AngualrInvoke
zAu.cmd1.ngInvoke = function (wgt) {
	if (wgt)
		return zAu.cmd1.invoke.apply(this, arguments);
	// if wgt is not found, we don't need to invoke in this case.
};

ng.Angular = zk.$extends(zk.Object, {
	$init: function (scope, element) {
		this.$element = element;
		var self = this;
		self.$scope = scope;
		this.$widget = zk.Widget.$(element);
	},
	$bind: function (key, expr) {
		var self = this;
		var scope = this.$scope;
		var has;
		if (scope.$$watchers) {
			for (var ws = scope.$$watchers, wsl = ws.length; wsl--;) {
				if (ws[wsl].exp == key) {
					has = true;
					break;
				}
			}
		}
		if (!has) {
			scope.$watch(key, function (newValue, oldValue) {
				zAu.send(new zk.Event(self.$widget, "onBindChange", {key: key, attnm: expr, value: newValue}, {toServer:true}), 38);
			}, true);
		}	
		//zAu.send(new zk.Event(this.$widget, "onBinding", {expr: expr}, {toServer:true}));
		return this;
	},
	$command: function (cmd, args) {
		this.$command0(cmd, args, true);
	},
	$command0: function (cmd, args, instant) {
		var wgt = this.$widget;
		if (!instant) {
			setTimeout(function () {
				zAu.send(new zk.Event(wgt, "onBindCommand", {cmd: cmd, args: args}, {toServer:true}));
			}, 38); // make command at the end of this request
		} else {
			zAu.send(new zk.Event(wgt, "onBindCommand", {cmd: cmd, args: args}, {toServer:true}));
		}
	},
	$globalCommand: function (cmd, args) {
		this.$globalCommand0(cmd, args, true);
	},
	$globalCommand0: function (cmd, args, instant) {
		var wgt = this.$widget;
		if (!instant) {
			setTimeout(function () {
				zAu.send(new zk.Event(wgt, "onBindGlobalCommand", {cmd: cmd, args: args}, {toServer:true}));
			}, 38); // make command at the end of this request
		} else {
			zAu.send(new zk.Event(wgt, "onBindGlobalCommand", {cmd: cmd, args: args}, {toServer:true}));
		}
	},
	$destroy: function () {
		this.$element = this.$scope = this.$widget = null;
	}
});
})(angular);