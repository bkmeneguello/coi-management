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
	
	var Entrada = Backbone.Model.extend({
		urlRoot: '/rest/entradas',
		defaults: function() {
			return {
				data: new Date(),
				paciente: new Pessoa(),
				valor: 0,
				tipo: null,
				produtos: new Produtos()
			};
		},
		parse: function(resp, options) {
			resp.data = $.datepicker.parseDate('yy-mm-dd', resp.data);
			resp.paciente = new Pessoa(resp.paciente);
			resp.produtos = new Produtos(resp.produtos);
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
			'quantidade': 'input',
			'buttonRemove': 'button.coi-action-remove'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.$el);
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
			this.collection.add(this.model);
			this.model = new Produto();
		}
	});
	
	var MeioPagamentoView = COI.FormItemView.extend({
		template: '#entrada_meio_pagamento_template',
		templateHelpers: {
			tipos_list: []
		},
		ui: {
			'select': 'select'
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
		}
	});
	
	var EntradaView = COI.FormView.extend({
		template: '#entrada_template',
		regions: {
			'paciente': '#paciente',
			'meioPagamento': '#meio-pagamento',
			'produtos': '#produtos'
		},
		modelEvents: {
			'change:paciente': 'renderPaciente',
			'change:produtos': 'renderProdutos'
		},
		initialize: function() {
			if (!this.model.isNew()) {
				this.model.fetch({
					success: this.onFetch
				});
			}
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['data'].converter = dateConverter;
			bindings['valor'].converter = moneyConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.renderPaciente();
			this.renderMeioPagamento();
			this.renderProdutos();
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		renderPaciente: function() {
			this.paciente.show(new COI.PessoaView({model: this.model.get('paciente'), label: 'Paciente:', attribute: 'paciente', required: true}));
		},
		renderMeioPagamento: function() {
			this.meioPagamento.show(new MeioPagamentoView({model: this.model, label: 'Meio Pagamento:', attribute: 'tipo'}));
		},
		renderProdutos: function() {
			this.produtos.show(new EntradaProdutosView({collection: this.model.get('produtos'), label: 'Produtos:', attribute: 'produtos'}));
		},
		onCancel: function(e) {
			Backbone.history.navigate('entradas', true);
		},
		onConfirm: function(e) {
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: this.onComplete});
			}
		},
		onComplete: function() {
			Backbone.history.navigate('entradas', true);
			_notifySuccess();
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