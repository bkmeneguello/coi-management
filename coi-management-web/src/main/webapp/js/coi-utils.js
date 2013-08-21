"use strict";

$.urlParam = function(name) {
	return (RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.search)||[,null])[1];
};

$.isBlank = function isBlank(str) {
    return (!str || /^\s*$/.test(str));
};

$.fn.disable = function() {
	return this.each(function() {
		$(this).attr('disabled', 'disabled');
	});
};

$.fn.enable = function() {
	return this.each(function() {
		$(this).removeAttr('disabled');
	});
};

$.widget('coi.validate', {
	options: {
		isValid: function() {
			return true;
		}
	},
	_create: function() {
		var that = this;
		this.element
			.on('focus', function() {
				that.reset();
			})
			.on('change blur', function() {
				that.validate();
			});
	},
	validate: function() {
		if (this.options.isValid(this.element.val())) {
			this.reset();
		} else {
			this.element.addClass('coi-validation-invalid');
			if (this.options.message) {
				this.element.tooltip({
					items: '*',
					content: this.options.message,
					tooltipClass: 'coi-tooltip-error'
				});
			}
		}
	},
	reset: function() {
		if (this.element.hasClass('coi-validation-invalid')) {
			this.element.removeClass('coi-validation-invalid');
			if (this.element.is(':ui-tooltip')) {
				this.element.tooltip('destroy');
			}
		}
	}
});

$.widget('coi.required', { //coi-validation-required
	_create: function() {
		this.element
			.validate({
				isValid: function(value) {
					return !$.isBlank(value);
				},
				message: 'Este campo é obrigatório'
			});
	}
});

$.widget('coi.input', {
	defaultElement: '<input>',
	_create: function() {
		this._draw();
	},
	_draw: function() {
		this.coiInput = this.element
			.addClass('coi-input-input')
			.attr('autocomplete', 'off')
			.wrap(this._coiInputHtml());
	},
	_coiInputHtml: function() {
		return '<span class="coi-input ui-widget ui-widget-content ui-corner-all"></span>';
	},
	_destroy: function() {
		this.element
			.removeClass('coi-input-input')
			.prop('disabled', false)
			.removeAttr('autocomplete');
		this.coiInput.replaceWith(this.element);
	},
	widget: function() {
		return this.coiInput;
	}
});

$.widget('coi.table', {
	defaultElement: '<table>',
	_create: function() {
		var that = this;
		if (this.element.is('table')) {
			this.element
				.addClass('ui-widget coi-table')
				.css({borderSpacing: '0', borderCollapse: 'collapse'});
			
			if (this.element.data('label')) {
				$('<label/>', {text: this.element.data('label')}).insertBefore(this.element);
			}
			
			if (this.options.buttons) {
				var uiDialogButtonPane = $("<div/>")
					.addClass("ui-dialog-buttonpane ui-widget-content ui-helper-clearfix")
					.insertAfter(this.element);
				
				var uiButtonSet = $("<div/>")
					.addClass("ui-dialog-buttonset")
					.appendTo(uiDialogButtonPane);
				
				var buttons = this.options.buttons;
				
				$.each(buttons, function(name, props) {
					var click, buttonOptions;
					props = $.isFunction(props) ? {click: props, text: name} : props;
					// Default to a non-submitting button
					props = $.extend({type: "button"}, props);
					// Change the context for the click callback to be the main element
					click = props.click;
					props.click = function() {
						click.apply( that.element[0], arguments );
					};
					buttonOptions = {
							icons: props.icons,
							text: props.showText
					};
					delete props.icons;
					delete props.showText;
					$("<button/>", props)
						.button(buttonOptions)
						.appendTo(uiButtonSet);
				});
			}
		}
	},
	_init: function() {
		if (this.element.is('table')) {
			var header = this.element.children('thead');
			header.find('th').addClass('ui-widget-header ui-state-default');
			
			var body = this.element.children('tbody');
			body.find('td').addClass('ui-widget-content');
			
			body.find('tr').hover(
				function() {
					$(this).children('td').addClass('ui-state-hover');
				},
				function() {
					$(this).children('td').removeClass('ui-state-hover');
				}
			)
			.click(function() {
				$(this).children('td').toggleClass('ui-state-highlight');
			});
		}
	}
});

$.widget('coi.form', {
	_init: function() {
		var form = this.element.closest('form');
		form.addClass('coi-form ui-dialog ui-widget ui-widget-content ui-corner-all');
		form.children('header').addClass('ui-dialog-titlebar ui-widget-header ui-corner-all ui-helper-clearfix');
		form.children('footer').addClass('ui-dialog-buttonpane ui-widget-content ui-helper-clearfix ui-corner-botton').wrapInner('<div class="ui-dialog-buttonset"/>');
	}
});