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
			return value ? (value.toFixed(2) + '').replace('.', ',') : value;
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
			return value ? (value.toFixed(3) + '').replace('.', ',') : value;
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
			return value ? 'R$ ' + (value.toFixed(2) + '').replace('.', ',') : value;
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
			return value ? (value.toFixed(2) + '').replace('.', ',') + '%' : value;
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
			return $.datepicker.formatDate('dd/mm/yy', value);
		} catch (e) {}
	case 'ViewToModel':
		try {
			return $.datepicker.parseDate('dd/mm/yy', value);
		} catch (e) {}
	}
};

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
			'click #estoque': 'estoque'
		},
		template: '#index_template',
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
				source: '/rest/pessoas',
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
			$.get('/rest/pessoas/next', {'prefix': codigo}, function(max) {
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
				buttons: {
					'Cancelar': function(e) {
						if (e && e.preventDefault){ e.preventDefault(); }
				        if (e && e.stopPropagation){ e.stopPropagation(); }
						that.triggerMethod('cancel', {
							view: that,
							model: that.model,
							collection: that.collection
						});
					},
					'Confirmar': function(e) {
						if (e && e.preventDefault){ e.preventDefault(); }
				        if (e && e.stopPropagation){ e.stopPropagation(); }
						that.triggerMethod('confirm', {
							view: that,
							model: that.model,
							collection: that.collection
						});
					}
				}
			});
		}, this);
	}
});

COI.GridView = Marionette.CompositeView.extend({
	search: false,
	constructor: function() {
		Marionette.CompositeView.prototype.constructor.apply(this, Array.prototype.slice.apply(arguments));
		this.listenTo(this, 'render', function() {
			if (!this.search) {
				this.ui.searchBar.hide();
			}
			this.$el.form();
			this.ui.table.table().css('width', '100%');
			this.ui.cancelButton.button();
			this.ui.createButton.button();
			this.ui.searchButton.button();
			this.ui.clearButton.button();
			this.ui.nextButton.button();
			this.ui.prevButton.button();
			
			var that = this;
			_.each(this.extras, function(extra) {
				that.ui.footer.children('div').append($('<button/>', {
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
		'click .coi-action-create': 'create',
		'click .coi-action-search': 'search',
		'click .coi-action-clear': 'clear',
		'click .coi-action-prev': 'prev',
		'click .coi-action-prox': 'next'
	},
	ui: {
		'table': 'table',
		'footer': 'footer',
		'cancelButton': 'footer button.coi-action-cancel',
		'createButton': 'footer button.coi-action-create',
		'searchBar': '.coi-paginator',
		'search': '.coi-paginator input.coi-input-search',
		'searchButton': '.coi-paginator button.coi-action-search',
		'clearButton': '.coi-paginator button.coi-action-clear',
		'prevButton': '.coi-paginator button.coi-action-prev',
		'nextButton': '.coi-paginator button.coi-action-prox'
	},
	onSearch: function(e) {
		this.collection.fetch({data: {term: this.ui.search.val()}});
	},
	onClear: function(e) {
		this.ui.search.val(null);
		this.collection.fetch();
	},
	onPrev: function(e) {
		this.collection.prevPage({data: {term: this.ui.search.val()}});
	},
	onNext: function(e) {
		this.collection.nextPage({data: {term: this.ui.search.val()}});
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