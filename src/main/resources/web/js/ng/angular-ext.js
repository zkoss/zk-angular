/* angular-ext.js

	Purpose:
		An extension for angularjs
	Description:
		
	History:
		Tue, Aug 05, 2014 10:25:49 AM, Created by jumperchen

Copyright (C)  Potix Corporation. All Rights Reserved.
*/
(function (angular) {

var App = angular.module('zk', []),
	IGNORE_COMMAND = {outer: 1, addAft: 1, addBfr: 1, addChd: 1, rm: 1};

// used to support '@command'
var ngEventDirectives = {};
jq.each(
	'click dblclick mousedown mouseup mouseover mouseout mousemove mouseenter mouseleave keydown keyup keypress submit focus blur copy cut paste'.split(' '),
	function(index, name) {
	var directiveName = jq.camelCase('ng-' + name);
	ngEventDirectives[directiveName] = ['$parse', function($parse) {
		return {
			priority: 1000,
		compile: function($element, attr) {
			attr[directiveName] = attr[directiveName].replace(/@command\(/g, '$command0(').replace(/@global-command\(/g, '$globalCommand0(');
		}
		};
	}];
	}
);
App.config(['$httpProvider', '$provide', function ($httpProvider, $provide) {
	$httpProvider.interceptors.push(['$q', '$rootScope',
		function ($q, $rootScope) {
			return {
				responseError: function (response) {
					zAu.showError('FAILED_TO_RESPONSE', response.status, response.statusText);
					return $q.reject(response);
				}
			};
	}]);
	
	 // configure httpBackend provider to handle zk-ng-include requests
	  $provide.decorator('$httpBackend', ['$delegate', function($httpBackend) {
		  var updateURI = zk.ajaxURI('/ngInclude', {au:true});
			// create function which overrides $httpBackend function
			return function(method, url, post, callback,
					headers, timeout, withCredentials,
					responseType) {
				return $httpBackend.apply(this, arguments);
			};
	}]);
}]);
var zkNgIncludeDirective = ['$http', '$templateCache', '$cacheFactory', '$anchorScroll', '$compile', '$animate', '$sce',
	function($http,	 $templateCache, $cacheFactory, $anchorScroll,	 $compile,	 $animate,	 $sce) {
	var $includeCache = $cacheFactory('zknginclude');
return {
	restrict: 'ECA',
	priority: 400,
	terminal: true,
	transclude: 'element',
	compile: function(element, attr, transclusion) {
		var srcExp = attr.zkNgInclude || attr.src,
			onloadExp = attr.onload || '',
			autoScrollExp = attr.autoscroll;

			function _unlink(wgt, child) {
				var p = child.previousSibling, n = child.nextSibling;
				if (p) p.nextSibling = n;
				else wgt.firstChild = n;
				if (n) n.previousSibling = p;
				else wgt.lastChild = p;
				child.nextSibling = child.previousSibling = child.parent = null;
	
				--wgt.nChildren;
			}
			function _link(wgt, child) {
				child.parent = wgt;
				var ref = wgt.lastChild;
				if (ref) {
					ref.nextSibling = child;
					child.previousSibling = ref;
					wgt.lastChild = child;
				} else {
					wgt.firstChild = wgt.lastChild = child;
				}
				++wgt.nChildren;
			}
			return function(scope, $element) {
				var changeCounter = 0,
					currentScope,
					currentElement;
			
				var cleanupLastIncludeContent = function() {
					if (currentScope) {
						currentScope.$destroy();
						currentScope = null;
					}
					if (currentElement) {
						var parent = zk.Widget.$(currentElement);
						for (var child = parent.firstChild; child ;) {
							child.unbind();
							var oldChild = child;
							child = child.nextSibling
							_unlink(parent, oldChild);
						}
						$animate.leave(currentElement);
						currentElement = null;
					}
				};
			
				scope.$watch($sce.parseAsResourceUrl(srcExp), function (src) {
					var afterAnimation = function() {
						if (autoScrollExp != 'undefined' && (!autoScrollExp || scope.$eval(autoScrollExp))) {
							$anchorScroll();
						}
					};
					var thisChangeId = ++changeCounter;
					
					if (src) {
						var lastIdx = src.lastIndexOf('/'),
							currentPath = src.substring(0, lastIdx + 1),
							cache = $includeCache.get(src);
						
						if (cache) {
							var newScope = scope.$new();
							transclusion(newScope, function(clone) {
								cleanupLastIncludeContent();

								currentScope = newScope;
								currentElement = clone;

								currentElement.html(cache.src);
								$animate.enter(currentElement, null, $element, afterAnimation);

								zk.afterMount(function () {

									var parent = zk.Widget.$(currentElement);
									jq(cache.wgts).each( function () {
										_link(parent, this);
										this.bind(dt);
									});
									currentScope.$currentPath = currentPath;
									$compile(currentElement.contents())(currentScope);
									currentScope.$emit('$includeContentLoaded');
									scope.$eval(onloadExp);
									var cmds = cache.cmds,
										dt = cache.dt;
									if (cmds.length) {
										zk.afterMount(function () {
										zAu.doCmds(dt.id, cmds);
										}, 5); // call later
									}
								}, -1);
							});
						} else {
							var wgt = zk.Widget.$($element),
								dt = wgt.desktop,
								ajaxURI = zk.ajaxURI('/ngInclude', {desktop: dt,au:true});
									$http({method: 'GET',
										url:ajaxURI, 
										params: {src: src, dtid: dt.id, uuid: wgt.uuid},
										headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
										transformResponse: function (data) {
											var zwgt = zk.Widget.$($element);
											if (thisChangeId !== changeCounter) return;
											if (!zwgt) return; // nothing to do, if the nested zk-ng-include happened with a src change in a wrong timing.
											
											var out = [],
												wgts = [],
												cmds = [];
											
											try {
												data = jq.evalJSON(data);
											} catch (e) { //not JSON Format, we assume it is a pure HTML
												var newScope = scope.$new();
												  transclusion(newScope, function(clone) {
													cleanupLastIncludeContent();
	
													currentScope = newScope;
													currentElement = clone;
													var cache = {src: data, wgts: wgts, cmds: cmds, dt: dt};
													$includeCache.put(src, cache);
													currentElement.html(cache.src);
													$animate.enter(currentElement, null, $element, afterAnimation);
	
													currentScope.$currentPath = currentPath;
													$compile(currentElement.contents())(currentScope);
													currentScope.$emit('$includeContentLoaded');
													scope.$eval(onloadExp);
												});
												return '';// return
											}
											
											var rdata = data[0][1];
											// remove the first item
											rdata.shift();
											
											jq(data).each(function () {
												if (!IGNORE_COMMAND[this[0]])
													cmds.push(this);
											});
											
											for (var i = 0, j = rdata.length, z = j; i < j; i++) {
												zkx_(rdata[i], function (newwgt) {
													z--;
													newwgt.redraw(out);
													wgts.push(newwgt);
													if (!z) {									
														var newScope = scope.$new();
														  transclusion(newScope, function(clone) {
															cleanupLastIncludeContent();
			
															currentScope = newScope;
															currentElement = clone;
															var cache = {src: out.join(''), wgts: wgts, cmds: cmds, dt:dt};
															$includeCache.put(src, cache);
															currentElement.html(cache.src);
															$animate.enter(currentElement, null, $element, afterAnimation);
			
															zk.afterMount(function () {
																var parent = zk.Widget.$(currentElement);
																jq(wgts).each( function () {
																	_link(parent, this);
																	this.bind(dt);
																});
																currentScope.$currentPath = currentPath;
																$compile(currentElement.contents())(currentScope);
																currentScope.$emit('$includeContentLoaded');
																scope.$eval(onloadExp);
																if (cmds.length) {
																	zk.afterMount(function () {
																	zAu.doCmds(dt.id, cmds);
																	}, 5); // call later
																}
															}, -1);
														});
													}
												});
											}
											return "";
										},
										cache: $templateCache
									}).error(function() {
									if (thisChangeId === changeCounter) cleanupLastIncludeContent();
								});
								scope.$emit('$includeContentRequested');
							}
					} else {
						cleanupLastIncludeContent();
					}
				});
			};
		}
	};
}];
App.directive(ngEventDirectives);
App.filter('encodeURL', function() {
  return function(input, scope) {
	  // check if not coming from classpath
	  if (input) {
		  if (!input.startsWith('~.') && !input.startsWith('/')) {
			  if (scope && scope.$currentPath)
				  input = scope.$currentPath + input;
		  }
	    return zk.ajaxURI(input);
	  }
	  return input;
  };
});
var _binderCache = {};
App.service('$binder', function () {
	var self = this;
	self.$init = function (scope, element) {
		if (element.length)
			element = element[0];
		var binder = _binderCache[element.id];
	
		if (!binder) {
			binder = self.$binder = new ng.Angular(scope, element);
			_binderCache[element.id] = binder;
			binder.$widget['@save'] = function(key, expr) {
				scope.$bind(key, expr);
			}
			binder.$widget['@load'] = function(key, value) {
				scope.__applying__ = true;
				try {
					scope.$apply(function() {
						try {
							scope[key] = value ? $.evalJSON(value) : null;
						} catch (e) {
							scope[key] = value;
						}
					});
				} finally {
					scope.__applying__ = false;
				}
			}
		}
		scope['$command0'] = function() {
			binder.$command0.apply(binder, arguments);
		};
		scope['$globalCommand0'] = function() {
			binder.$globalCommand0.apply(binder, arguments);
		};
		scope['$command'] = function() {
			binder.$command.apply(binder, arguments);
		};
		scope['$globalCommand'] = function() {
			binder.$globalCommand.apply(binder, arguments);
		};
		
		// make our $destroy function is the last one. 
		setTimeout(function () {
			scope.$on('$destroy', function() {
				if (jq.isFunction(scope.$beforeDestroy)) {
					scope.$beforeDestroy();
				}
				binder.$destroy();
				delete _binderCache[element.id];
				binder = null;
			});
		}, 50);
		
		scope.$bind = function() {
			return binder.$bind.apply(binder, arguments);
		};
	}
}).directive('ngChange', function() {
	return {
		restrict : 'A',
		priority : 1000,
		require : 'ngModel',
		link : function(scope, element, attr, ctrl) {
			attr.ngChange = attr.ngChange.replace(/@command\(/g,
					'$command0(').replace(/@global-command\(/g,
					'$globalCommand0(');
		}
	}
}).directive('zkNgInclude', zkNgIncludeDirective);
	
	
})(angular);