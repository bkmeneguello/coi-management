"use strict";

COI.module("Movimento", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Produto = Backbone.Model.extend({
		defaults: {
			codigo: null,
			descricao: null,
			quantidade: 1
		}
	});

	var Produtos = Backbone.Collection.extend({
		url: 'rest/produtos',
		model: Produto
	});

	var Movimento = Backbone.Model.extend({
		urlRoot: 'rest/estoque',
		defaults: function() {
			return {
				data: new Date(),
				tipo: null,
				produtos: new Produtos()
			};
		},
		parse: function(resp, options) {
			resp.produtos = new Produtos(resp.produtos, {parse: true});
			return resp;
		},
		validate: function(attrs, options) {
			if (attrs.produtos.length == 0) {
				return "Um movimento deve referenciar pelo menos um produto";
			}
		}
	});
	
	var ProdutoView = Marionette.ItemView.extend({
		tagName: 'tr',
		template: '#movimento_produto_template',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		ui: {
			'quantidade': 'input',
			'buttonRemove': 'button.coi-action-remove'
		},
		triggers: {
			'click .coi-action-remove': 'remove'
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.$el);
			this.ui.quantidade.input();
			this.ui.buttonRemove.button();
		},
		onRemove: function(e) {
			this.model.collection.remove(this.model);
		}
	});
	
	var ProdutosView = COI.FormCompositeView.extend({
		template: '#movimento_produtos_template',
		itemViewContainer: 'table',
		itemView: ProdutoView,
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
				source: 'rest/categorias/produtos/estocaveis',
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
	
	var MovimentoView = COI.FormView.extend({
		template: '#movimento_template',
		ui: {
			'tipo': '#tipo'
		},
		regions: {
			'produtos': '#produtos'
		},
		modelEvents: {
			'change:produtos': 'renderProdutos'
		},
		triggers: {
			
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
			
			this.ui.tipo.input().required();
			this.renderProdutos();
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		renderProdutos: function() {
			this.produtos.show(new ProdutosView({collection: this.model.get('produtos')}));
		},
		onCancel: function(e) {
			Backbone.history.navigate('estoque', true);
		},
		onConfirm: function(e) {
			if (_validate(e.view)) {
				if (!e.model.save(null, {wait: true, success: e.view.onComplete, error: e.view.onError})) {
					_notifyWarning(e.model.validationError);
				}
			}
		},
		onComplete: function(e) {
			Backbone.history.navigate('estoque', true);
			_notifySuccess();
		},
		onError: function(model, resp, options) {
			_notifyError(resp.responseText);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('movimento', function() {
			COI.body.show(new MovimentoView({model: new Movimento()}));
		});
		COI.router.route('movimento(/:id)', function(id) {
			COI.body.show(new MovimentoView({model: new Movimento({id: id})}));
		});
	});
});