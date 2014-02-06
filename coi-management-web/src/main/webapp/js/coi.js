"use strict";

function _validate(element) {
	element.$(':coi-validate:visible').validate('validate');
	if(element.$('.coi-validation-invalid:visible').length) {
		_notifyValidation();
		return false;
	}
	return true;
}

function _notifyError(text) {
	noty({text: text, type: 'error'});
}

function _notifyWarning(text) {
	noty({text: text, type: 'warning', timeout: 5000});
}

function _notifyValidation() {
	_notifyWarning('Verifique todos os campos');
}

function _notifySuccess() {
	noty({text: 'Registro incluido com sucesso', type: 'success', timeout: 2000});
}

function _notifyDelete() {
	noty({text: 'Registro excluido com sucesso', type: 'success', timeout: 2000});
}

function _notifyDeleteFailure() {
	_notifyError('Falha na exclusão do registro');
}

function _notifyUpdateFailure() {
	_notifyError('Falha no cadastro do registro');
}

function _promptDelete(confirm) {
	noty({
		text: 'Deseja realmente excluir o registro?',
		layout: 'center',
		buttons: [
		    {
			    addClass: 'btn',
			    text: 'Cancelar',
			    onClick: function($noty) {
			        $noty.close();
		    	}
		 	},
			{
				addClass: 'btn', 
				text: 'Confirmar', 
				onClick: function($noty) {
					confirm();
					$noty.close();					
				}
		    }
		],
		callback: {
			onShow: function() {
				$('button.btn').button();
			}
		}
	});
}

function decimalConverter(direction, value) {
	switch(direction) {
	case 'ModelToView':
		try {
			return value || value == 0 ? (value.toFixed(2) + '').replace('.', ',') : value;
		} catch (e) {}
	case 'ViewToModel':
		try {
			return Number(value.replace('.', '').replace(',', '.').replace(/[^0-9\-\.]+/g,""));
		} catch (e) {}
	}
};

function decimal3Converter(direction, value) {
	switch(direction) {
	case 'ModelToView':
		try {
			return value || value == 0 ? (value.toFixed(3) + '').replace('.', ',') : value;
		} catch (e) {}
	case 'ViewToModel':
		try {
			return Number(value.replace('.', '').replace(',', '.').replace(/[^0-9\-\.]+/g,""));
		} catch (e) {}
	}
};

function moneyConverter(direction, value) {
	switch(direction) {
	case 'ModelToView':
		try {
			return value || value == 0 ? 'R$ ' + (value.toFixed(2) + '').replace('.', ',') : value;
		} catch (e) {}
	case 'ViewToModel':
		try {
			return Number(value.replace('.', '').replace(',', '.').replace(/[^0-9\-\.]+/g,""));
		} catch (e) {}
	}
};

function percentageConverter(direction, value) {
	switch(direction) {
	case 'ModelToView':
		try {
			return value || value == 0 ? (value.toFixed(2) + '').replace('.', ',') + '%' : value;
		} catch (e) {}
	case 'ViewToModel':
		try {
			return Number(value.replace('.', '').replace(',', '.').replace(/[^0-9\-\.]+/g,""));
		} catch (e) {}
	}
};

function dateConverter(direction, value) {
	switch(direction) {
	case 'ModelToView':
		try {
			if (!value) return value;
			if (typeof(value) == 'string') value = strToTimestamp(value);
			return formatDateStr(new Date(value));
		} catch (e) {}
	case 'ViewToModel':
		try {
			var date = parseDateStr(value);
			return new Date(date[2], date[1] - 1, date[0], 0, 0, 0, 0).getTime();
		} catch (e) {}
	}
};

function strToTimestamp(value) {
	return new Date(value).setHours(new Date(value).getHours() + new Date(value).getTimezoneOffset() / 60);
}

function toTimestamp(value) {
	if (!value) return value;
	
	switch(typeof(value)) {
		case 'string':
			return strToTimestamp(value);
		case 'object':
			return value.getTime();
	}
	return value;
}

function timestampConverter(direction, value) {
	switch(direction) {
	case 'ModelToView':
		try {
			if (!value) return value;
			var date = new Date(strToTimestamp(value));
			return formatDateStr(date) + ' ' + formatTimeStr(date);
		} catch (e) {}
	case 'ViewToModel':
		try {
			var date_time = value.split(' ');
			var date = parseDateStr(date_time[0]);
			var time = date_time.length > 1 ? date_time[1].split(':') : [0, 0];
			time[0] = parseIntSafe(time[0]);
			time[1] = parseIntSafe(time[1]);
			return new Date(date[2], date[1] - 1, date[0], time[0], time[1], 0, 0).getTime();
		} catch (e) {}
	}
}

function formatDateStr(date) {
	return pad(date.getDate(), 2) + '/' + pad(date.getMonth() + 1, 2) + '/' + date.getFullYear();
}

function formatTimeStr(date) {
	return pad(date.getHours(), 2) + ":" + pad(date.getMinutes(), 2);
}

function parseDateStr(value) {
	var date = value.split('/');
	if (date.length < 3) {
		var d = value.replace('/', '');
		date = [d.substr(0,2), d.substr(2,2), d.substr(4)];
	}
	date[2] = parseIntSafe(date[2]);
	if (date[2] < 30) {
		date[2] += 2000;
	} else if (date[2] < 100) {
		date[2] += 1900;
	}
	date[1] = parseIntSafe(date[1]);
	date[0] = parseIntSafe(date[0]);
	return date;
};

function parseIntSafe(value) {
	if (!value) return 0;
	return parseInt(value);
};

function pad(n, width, z) {
	z = z || '0';
	n = n + '';
	return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

var COI = new Backbone.Marionette.Application();

COI.addRegions({
	'body': 'body'
});

COI.addInitializer(function (options) {
	this.router = new Backbone.Router();
});

COI.on('start', function(options) {
	Backbone.history.start();
});

COI.module('Index', function(Module, COI, Backbone, Marionette, $, _) {
	var IndexView = Marionette.ItemView.extend({
		events: {
			'click #categorias': 'categorias',
			'click #pessoas': 'pessoas',
			'click #entradas': 'entradas',
			'click #cheques': 'cheques',
			'click #laudos': 'laudos',
			'click #estoque': 'estoque',
			'click #pagamentos': 'pagamentos'
		},
		template: '#index_template',
		id: 'main',
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.$('button').button();
		},
		categorias: function(e) {
			Backbone.history.navigate('categorias', true);
		},
		pessoas: function(e) {
			Backbone.history.navigate('pessoas', true);
		},
		entradas: function(e) {
			Backbone.history.navigate('entradas', true);
		},
		cheques: function(e) {
			Backbone.history.navigate('cheques', true);
		},
		laudos: function(e) {
			Backbone.history.navigate('laudos', true);
		},
		estoque: function(e) {
			Backbone.history.navigate('estoque', true);
		},
		pagamentos: function(e) {
			Backbone.history.navigate('pagamentos', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('', function() {
			COI.body.show(new IndexView());
		});
	});
});

COI.FormItemView = Marionette.ItemView.extend({
	constructor: function() {
		Marionette.ItemView.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
	},
	tagName: 'dl',
	className: 'coi-form-item',
	modelBinder: function() {
		return new Backbone.ModelBinder();
	},
	serializeData: function() {
		var data = Marionette.ItemView.prototype.serializeData.apply(this, Array.prototype.slice.apply(arguments));
		return _.extend(data, {
			name: this.options.attribute,
			label: this.options.label
		});
	},
	initialize: function() {
		_.bindAll(this);
	}
});

COI.FormCompositeView = Marionette.CompositeView.extend({
	constructor: function() {
		Marionette.CompositeView.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
	},
	tagName: 'dl',
	className: 'coi-form-item',
	modelBinder: function() {
		return new Backbone.ModelBinder();
	},
	serializeData: function() {
		var data = Marionette.ItemView.prototype.serializeData.apply(this, Array.prototype.slice.apply(arguments));
		return _.extend(data, {
			name: this.options.attribute,
			label: this.options.label
		});
	},
	initialize: function() {
		_.bindAll(this);
	}
});

COI.TextView = COI.FormItemView.extend({
	constructor: function() {
		COI.FormItemView.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
		this.listenTo(this, 'render', function() {
			this.$('input[type=text]').input();
			if (this.options.required) {
				this.$('input[type=text]').required();
			}
		}, this);
	},
	template: '#coi_view_text_template'
});

COI.TextAreaView = COI.FormItemView.extend({
	constructor: function() {
		COI.FormItemView.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
		this.listenTo(this, 'render', function() {
			this.$('textarea').input();
			if (this.options.required) {
				this.$('textarea').required();
			}
		}, this);
	},
	template: '#coi_view_textarea_template'
});

COI.MoneyView = COI.TextView.extend({
	constructor: function() {
		COI.TextView.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
		this.listenTo(this, 'render', function() {
			this.ui.input.money();
		}, this);
	}
});

COI.PessoaView = Marionette.ItemView.extend({
	constructor: function() {
		Marionette.ItemView.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
		this.listenTo(this, 'render', function() {
			this.modelBinder().bind(this.model, this.el);
			
			this.ui.pessoaNewSession.hide();
			
			var that = this;
			var model = this.model;
			this.ui.input.input();
			this.ui.input.autocomplete({
				source: 'rest/pessoas',
				minLength: 3,
				appendTo: this.ui.input.closest('.coi-form-item'),
				response: function(event, ui) {
					$.each(ui.content, function(index, element) {
						element.label = '[' + element.codigo + '] ' + element.nome;
						element.value = element.nome;
					});
				},
				select: function(event, ui) {
					var element = ui.item;
					model.set(_.omit(element, 'label', 'value'));
				}
			})
			.change(function() {
				if ($(this).val() != model.get('nome')) {
					$(this).val(null);
				}
			})
			.blur(function() {
				if ($.isBlank($(this).val())) {
					model.clear();
				} else if (model.isNew()) {
					$(this).val(null).blur();
				}
			})
			.autocomplete('widget')
			.css('z-index', 100);
			if (this.options.required) {
				this.ui.input.required();
			}
				
			this.ui.buttonNew = $('<button/>', {'class': 'coi-action-include', text: 'Novo'})
				.css('marginLeft', '5px')
				.click(function(e) {
					if (e && e.preventDefault){ e.preventDefault(); }
			        if (e && e.stopPropagation){ e.stopPropagation(); }
					that.triggerMethod('create:pessoa', {
						view: that,
						model: that.model,
						collection: that.collection
					});
				})
				.button();
			this.ui.input.parent().after(this.ui.buttonNew);
			
			this.ui.inputNewCodigo.input().required();
			this.ui.inputNewNome.input().required();
			this.ui.buttonCancel.button();
		}, this);
	},
	modelBinder: function() {
		return new Backbone.ModelBinder();
	},
	serializeData: function() {
		var data = Marionette.ItemView.prototype.serializeData.apply(this, Array.prototype.slice.apply(arguments));
		return _.extend(data, {
			name: this.options.attribute,
			label: this.options.label
		});
	},
	template: '#coi_view_pessoa_template',
	ui: {
		'label': 'dt',
		'input': 'input[type=text].coi-view-pessoa-nome',
		'pessoaSession': 'div.coi-view-pessoa',
		'pessoaNewSession': 'div.coi-view-pessoa-new',
		'inputNewCodigo': 'input[type=text].coi-view-pessoa-new-codigo',
		'inputNewNome': 'input[type=text].coi-view-pessoa-new-nome',
		'buttonCancel': 'button.coi-action-cancel'
	},
	triggers: {
		'click .coi-action-cancel': 'cancel',
		'keyup input[type=text].coi-view-pessoa-new-codigo': 'changeCodigo'
	},
	onRender: function() {
		if ($.isBlank(this.options.label)) {
			this.ui.label.hide();
		}
	},
	onChangeCodigo: function(e) {
		var input = this.ui.inputNewCodigo;
		var codigo = input.val().toUpperCase();
		if (codigo.length == 1) {
			$.get('rest/pessoas/next', {'prefix': codigo}, function(max) {
				input.val(codigo + "-" + max);
				input.get(0).setSelectionRange(2, input.val().length);
				input.change();
			});
		}
	},
	onCreatePessoa: function(e) {
		this.ui.pessoaSession.hide();
		e.model.clear();
		this.ui.inputNewCodigo.val(null);
		this.ui.inputNewNome.val(null);
		this.ui.pessoaNewSession.show();
		this.ui.inputNewCodigo.focus();
	},
	onCancel: function(e) {
		this.ui.pessoaNewSession.hide();
		e.model.clear();
		this.ui.input.val(null);
		this.ui.pessoaSession.show();
		this.ui.input.focus();
	}
});

COI.Window = Marionette.Layout.extend({
	views: {
		'.coi-view-text': COI.TextView,
		'.coi-view-textarea': COI.TextAreaView,
		'.coi-view-money': COI.MoneyView
	},
	constructor: function() {
		Marionette.Layout.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
		this.listenTo(this, 'render', function() {
			var that = this;
			$.each(this.views, function(className, view) {
				that.$(className).each(function(index, element){
					element = $(element);
					var attribute = element.data('attribute');
					var label = element.data('label');
					var required = element.data('required');
					var id = element.uniqueId().attr('id');
					that.addRegion(name, '#'+ id).show(new view({
						model: that.model,
						attribute: attribute, 
						label: label, 
						required: required
					}));
				});
			});
		}, this);
	},
	modelBinder: function() {
		return new Backbone.ModelBinder();
	}
});

COI.FormView = COI.Window.extend({
	constructor: function() {
		COI.Window.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
		var that = this;
		this.listenTo(this, 'render', function() {
			this.$el.form();
			this.$('button.coi-action-cancel').button().click(function(e) {
				if (e && e.preventDefault){ e.preventDefault(); }
		        if (e && e.stopPropagation){ e.stopPropagation(); }
				that.triggerMethod('cancel', {
					view: that,
					model: that.model,
					collection: that.collection
				});
			});
			this.$('button.coi-action-confirm').button().click(function(e) {
				if (e && e.preventDefault){ e.preventDefault(); }
		        if (e && e.stopPropagation){ e.stopPropagation(); }
				that.triggerMethod('confirm', {
					view: that,
					model: that.model,
					collection: that.collection
				});
			});
		}, this);
	},
	tagName: 'form'
});

COI.PopupFormView = COI.Window.extend({
	constructor: function() {
		COI.Window.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
		this.listenTo(this, 'render', function() {
			var that = this;
			this.$el.dialog({
				title: this.header,
				dialogClass: 'no-close',
				height: this.height,
				width: this.width,
				modal: true,
				buttons: [
					{
						text: 'Cancelar',
						'class': 'coi-action-cancel',
						click: function(e) {
							if (e && e.preventDefault){ e.preventDefault(); }
					        if (e && e.stopPropagation){ e.stopPropagation(); }
							that.triggerMethod('cancel', {
								view: that,
								model: that.model,
								collection: that.collection
							});
						}
					},
					{
						text: 'Confirmar',
						'class': 'coi-action-confirm',
						click: function(e) {
							if (e && e.preventDefault){ e.preventDefault(); }
					        if (e && e.stopPropagation){ e.stopPropagation(); }
							that.triggerMethod('confirm', {
								view: that,
								model: that.model,
								collection: that.collection
							});
						}
					}
				]
			});
		}, this);
	}
});

COI.SimpleFilterView = Backbone.View.extend({
	initialize: function() {
		_.bindAll(this);
	},
	render: function() {
		this.searchInput = $('<input/>' , {type: 'text', 'class': 'coi-input-search'});
		this.$el.append(this.searchInput);
		this.searchInput.input();
		this.$el
			.append($('<button/>' , {text: 'Pesquisar', 'class': 'coi-action-search', click: this.search}).button())
			.append($('<button/>' , {text: 'Limpar', 'class': 'coi-action-clear', click: this.clear}).button())
			.append($('<button/>' , {text: 'Anterior', 'class': 'coi-action-prev', click: this.prev}).button())
			.append($('<button/>' , {text: 'Próxima', 'class': 'coi-action-prox', click: this.next}).button());
	},
	search: function(e) {
		if (e && e.preventDefault){ e.preventDefault(); }
        if (e && e.stopPropagation){ e.stopPropagation(); }
		this.collection.fetch({data: {term: this.searchInput.val()}});
	},
	clear: function(e) {
		if (e && e.preventDefault){ e.preventDefault(); }
        if (e && e.stopPropagation){ e.stopPropagation(); }
		this.searchInput.val(null);
		this.collection.fetch();
	},
	prev: function(e) {
		if (e && e.preventDefault){ e.preventDefault(); }
        if (e && e.stopPropagation){ e.stopPropagation(); }
		this.collection.prevPage({data: {term: this.searchInput.val()}});
	},
	next: function(e) {
		if (e && e.preventDefault){ e.preventDefault(); }
        if (e && e.stopPropagation){ e.stopPropagation(); }
		this.collection.nextPage({data: {term: this.searchInput.val()}});
	}
});

COI.GridView = Marionette.CompositeView.extend({
	searchView: null,
	constructor: function() {
		Marionette.CompositeView.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
		this.listenTo(this, 'render', function() {
			if (this.searchView) {
				var paginator = this.$('.coi-paginator').show();
				this._search = new this.searchView({el: paginator, collection: this.collection});
				this._search.render();
			}
			this.$el.form();
			this.ui.table.table().css('width', '100%');
			this.ui.cancelButton.button();
			this.ui.createButton.button();
			
			var that = this;
			_.each(this.extras, function(extra) {
				that.ui.footer.children('div').append($('<button/>', {
					id: extra.id,
					'class': extra['class'],
					text: extra.text,
					click: function(e) {
						if (e && e.preventDefault){ e.preventDefault(); }
				        if (e && e.stopPropagation){ e.stopPropagation(); }
						that.triggerMethod(extra.trigger, {
							view: that,
							model: that.model,
							collection: that.collection
						});
					}
				}).button());
			});
		});
	},
	template: '#coi_grid_view_template',
	tagName: 'form',
	itemViewContainer: 'tbody',
	triggers: {
		'click .coi-action-cancel': 'cancel',
		'click .coi-action-create': 'create'
	},
	ui: {
		'table': 'table',
		'footer': 'footer',
		'cancelButton': 'footer button.coi-action-cancel',
		'createButton': 'footer button.coi-action-create'
	}
});

COI.ActionRowView = Marionette.ItemView.extend({
	constructor: function() {
		Marionette.ItemView.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
		this.listenTo(this, 'render', function() {
			var that = this;
			this.$('.coi-action-update').click(function(e) {
				if (e && e.preventDefault) e.preventDefault();
				if (e && e.stopPropagation) e.stopPropagation();
				var args = {
					view : that,
					model : that.model,
					collection : that.collection
				};
				that.triggerMethod('update', args);
			}).button();
			this.$('.coi-action-delete').click(function(e) {
				if (e && e.preventDefault) e.preventDefault();
				if (e && e.stopPropagation) e.stopPropagation();
				var args = {
					view : that,
					model : that.model,
					collection : that.collection
				};
				that.triggerMethod('delete', args);
			}).button();
		});
	},
	tagName: 'tr'
});