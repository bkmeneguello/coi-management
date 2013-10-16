"use strict";

COI.module("Entrada", function(Module, COI, Backbone, Marionette, $, _) {

	var Pessoa = Backbone.Model.extend({
		defaults: {
			nome: null,
			codigo: null
		}
	});
	
	var Produto = Backbone.Model.extend({
		defaults: {
			quantidade: 1
		}
	});
	
	var Produtos = Backbone.Collection.extend({
		model: Produto
	});
	
	var Cheque = Backbone.Model.extend({
		defaults: function() {
			return {
				numero: null,
				conta: null,
				agencia: null,
				banco: null,
				documento: null,
				valor: null,
				dataDeposito: null,
				observacao: null,
				cliente: new Pessoa()
			};
		},
		parse: function(resp, options) {
			resp.dataDeposito = resp.dataDeposito ? $.datepicker.parseDate('yy-mm-dd', resp.dataDeposito) : null;
			resp.cliente = new Pessoa(resp.cliente, {parse: true});
			return resp;
		}
	});
	
	var Cheques = Backbone.Collection.extend({
		model: Cheque
	});
	
	var Parte = Backbone.Model.extend({
		defaults: function() {
			return {
				pessoa: new Pessoa(),
				parte: null
			};
		},
		parse: function(resp, options) {
			resp.pessoa = new Pessoa(resp.pessoa, {parse: true});
			return resp;
		}
	});
	
	var Partes = Backbone.Collection.extend({
		model: Parte
	});
	
	var Entrada = Backbone.Model.extend({
		urlRoot: '/rest/entradas',
		defaults: function() {
			return {
				data: new Date(),
				paciente: new Pessoa(),
				tipo: null,
				produtos: new Produtos(),
				partes: new Partes(),
				cheques: new Cheques()
			};
		},
		parse: function(resp, options) {
			resp.data = $.datepicker.parseDate('yy-mm-dd', resp.data);
			resp.paciente = new Pessoa(resp.paciente, {parse: true});
			resp.produtos = new Produtos(resp.produtos, {parse: true});
			resp.partes = new Partes(resp.partes, {parse: true});
			resp.cheques = new Cheques(resp.cheques, {parse: true});
			return resp;
		}
	});
	
	var EntradaProdutoView = Marionette.ItemView.extend({
		tagName: 'tr',
		template: '#entrada_produto_template',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		triggers: {
			'click .coi-action-remove': 'remove'
		},
		ui: {
			'desconto': 'input[name=desconto]',
			'quantidade': 'input[name=quantidade]',
			'buttonRemove': 'button.coi-action-remove'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['desconto'].converter = moneyConverter;
			this.modelBinder().bind(this.model, this.$el, bindings);
			this.ui.desconto.input();
			this.ui.quantidade.input();
			this.ui.buttonRemove.button();
			//this.ui.quantidade.spinner({min: 1}); //TODO
		},
		onRemove: function(e) {
			this.model.collection.remove(this.model);
		}
	});
	
	var EntradaProdutosView = COI.FormCompositeView.extend({
		template: '#entrada_produtos_template',
		itemViewContainer: 'tbody',
		itemView: EntradaProdutoView,
		ui: {
			'input': 'input[type=text].coi-view-produto',
			'buttonInclude': 'button.coi-action-include',
			'table': 'table'
		},
		triggers: {
			'click .coi-action-include': 'include'
		},
		initialize: function() {
			this.model = new Produto();
		},
		onRender: function() {
			this.ui.input.input();
			this.ui.buttonInclude.button();
			this.ui.table.table().css('width', '100%');
			var that = this;
			this.ui.input.autocomplete({
				source: '/rest/categorias/produtos',
				minLength: 3,
				appendTo: this.ui.input.closest('.coi-form-item'),
				response: function(event, ui) {
					$.each(ui.content, function(index, element) {
						element.label = '[' + element.codigo + '] ' + element.descricao;
						element.value = element.descricao;
					});
				},
				select: function(event, ui) {
					that.model = new Produto(_.omit(ui.item, 'label', 'value'));
				}
			})
			.change(function() {
				if ($(this).val() != that.model.get('descricao')) {
					$(this).val(null);
				}
			})
			.blur(function() {
				if ($.isBlank($(this).val())) {
					that.model = new Produto();
				} else if (that.model.isNew()) {
					$(this).val(null).blur();
				}
			})
			.autocomplete('widget')
			.css('z-index', 100);
		},
		onInclude: function(e) {
			this.ui.input.val(null);
			this.collection.add(this.model);
			this.model = new Produto();
		}
	});
	
	var ChequeView = COI.Window.extend({
		template: '#entrada_meio_pagamento_cheque_template',
		className: 'coi-view-cheque coi-form-item',
		regions: {
			'emissor': '#emissor'
		},
		triggers: {
			'click .coi-action-remove': 'remove'
		},
		ui: {
			'cheque': '.coi-view-cheque',
			'buttonRemove': '.coi-action-remove'
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['dataDeposito'].converter = dateConverter;
			bindings['valor'].converter = moneyConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.ui.buttonRemove.button();
			
			this.emissor.show(new COI.PessoaView({model: this.model.get('cliente'), label: 'Emissor:', attribute: 'cliente', required: true}));
			
			if (!this.model.isNew()) {
				this.$('input,textarea').disable();
			}
		},
		onRemove: function(e) {
			this.model.collection.remove(this.model);
		}
	});
	
	var ChequesView = Marionette.CollectionView.extend({
		itemView: ChequeView
	});
	
	var MeioPagamentoView = Marionette.Layout.extend({
		template: '#entrada_meio_pagamento_template',
		templateHelpers: {
			tipos_list: []
		},
		regions: {
			'cheques': '#cheques'
		},
		ui: {
			'select': 'select',
			'buttonCreate': '.coi-action-create'
		},
		triggers: {
			'click .coi-action-create': 'create'
		},
		modelEvents: {
			'change:tipo': 'updateTipo'
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
		initialize: function() {
			var that = this;
			this.templateHelpers.tipos_list.length = 0;
			$.get('/rest/entradas/meios', function(meios) {
				$.each(meios, function(index, value) {
					that.templateHelpers.tipos_list.push(value);
				});
				that.render();
			});
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.el);
			this.ui.select.input();
			this.ui.buttonCreate.button().hide();
			this.updateTipo(this.model, this.ui.select.val());
		},
		updateTipo: function(model, value) {
			if ('Cheque' == value) {
				this.cheques.show(new ChequesView({collection: this.model.get('cheques')}));
				this.ui.buttonCreate.show();
			} else {
				this.cheques.close();
				this.ui.buttonCreate.hide();
			}
		},
		onCreate: function(e) {
			this.model.get('cheques').add(new Cheque());
		}
	});
	
	var EntradaParteView = Marionette.ItemView.extend({
		template: '#entrada_parte_template',
		ui: {
			'input': '.coi-view-text',
			'pessoa': '.coi-view-pessoa',
			'buttonRemove': '.coi-action-remove'
		},
		triggers: {
			'click .coi-action-remove': 'remove'
		},
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.$el);
			this.ui.input.input();
			new COI.PessoaView({el: this.ui.pessoa, model: this.model.get('pessoa'), label: this.model.get('parte') + ':', attribute: 'pessoa', required: true}).render();
			this.ui.buttonRemove.button();
		},
		onRemove: function(e) {
			this.model.collection.remove(this.model);
		}
	});
	
	var EntradaPartesView = Marionette.CompositeView.extend({
		template: '#entrada_partes_template',
		itemView: EntradaParteView,
		itemViewContainer: '#partes-itens',
		ui : {
			'parte': '#parte',
			'buttonInclude': '.coi-action-include'
		},
		triggers: {
			'click .coi-action-include': 'include'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.ui.parte.input();
			this.ui.buttonInclude.button();
			this.ui.parte.autocomplete({
				source: '/rest/partes/comissionadas',
				appendTo: this.ui.parte.closest('.coi-form-item'),
				response: function(event, ui) {
					$.each(ui.content, function(index, element) {
						element.label = element.descricao;
						element.value = element.descricao;
					});
				}
			})
			.autocomplete('widget')
			.css('z-index', 100);
		},
		onInclude: function(e) {
			this.collection.add(new Parte({
				parte: this.ui.parte.val()
			}));
			this.ui.parte.val(null);
		}
	});
	
	var EntradaView = COI.FormView.extend({
		template: '#entrada_template',
		regions: {
			'produtos': '#produtos',
			'paciente': '#paciente',
			'partes': '#partes',
			'meioPagamento': '#meio-pagamento',
		},
		modelEvents: {
			'change:produtos': 'renderProdutos',
			'change:paciente': 'renderPaciente',
			'change:partes': 'renderPartes'
		},
		initialize: function() {
			if (!this.model.isNew()) {
				this.model.fetch();
			}
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['data'].converter = dateConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.renderPaciente();
			this.renderProdutos();
			this.renderMeioPagamento();
			this.renderPartes();
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		renderMeioPagamento: function() {
			this.meioPagamento.show(new MeioPagamentoView({model: this.model, label: 'Meio Pagamento:', attribute: 'tipo'}));
		},
		renderProdutos: function() {
			this.produtos.show(new EntradaProdutosView({collection: this.model.get('produtos'), label: 'Produtos:', attribute: 'produtos'}));
		},
		renderPaciente: function() {
			this.paciente.show(new COI.PessoaView({model: this.model.get('paciente'), label: 'Paciente:', attribute: 'paciente', required: true}));
		},
		renderPartes: function() {
			this.partes.show(new EntradaPartesView({collection: this.model.get('partes'), label: 'Partes:', attribute: 'partes'}));
		},
		onCancel: function(e) {
			Backbone.history.navigate('entradas', true);
		},
		onConfirm: function(e) {
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: this.onComplete, error: this.onError});
			}
		},
		onComplete: function(e) {
			Backbone.history.navigate('entradas', true);
			_notifySuccess();
		},
		onError: function(model, resp, options) {
			_notifyError(resp.responseText);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('entrada', function() {
			COI.body.show(new EntradaView({model: new Entrada()}));
		});
		COI.router.route('entrada(/:id)', function(id) {
			COI.body.show(new EntradaView({model: new Entrada({id: id})}));
		});
	});
});