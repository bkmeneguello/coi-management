"use strict";

COI.module("Categoria", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Parte = Backbone.Model.extend({
		defaults: {
			descricao: null
		}
	});

	var PartesComissionadas = Backbone.Collection.extend({
		url: '/rest/partes/comissionadas',
		model: Parte
	});
	
	var Produto = Backbone.Model.extend({
		defaults: {
			codigo: null,
			descricao: null,
			custo: 0,
			preco: 0
		}
	});

	var Produtos = Backbone.Collection.extend({
		model: Produto
	});

	var Comissao = Backbone.Model.extend({
		defaults: {
			parte: null,
			porcentagem: 0
		}
	});

	var Comissoes = Backbone.Collection.extend({
		model: Comissao
	});
	
	var Categoria = Backbone.Model.extend({
		urlRoot: '/rest/categorias',
		defaults: function() {
			return {
				descricao: null,
				produtos: new Produtos(),
				comissoes: new Comissoes()
			};
		},
		parse: function(resp, options) {
			resp.produtos = new Produtos(resp.produtos);
			resp.comissoes = new Comissoes(resp.comissoes);
			return resp;
		},
		validate: function(attrs, options) {
			var total = 0;
			attrs.comissoes.each(function(element, index, list) {
				total += element.get('porcentagem');
			});
			if (total != 100) {
				return 'Os percentuais de comissão devem somar 100%';
			}
		}
	});
	
	var CategoriaProdutoView = COI.PopupFormView.extend({
		template: '#categoria_produto_template',
		header: 'Produto',
		width: 450,
		height: 500,
		initialize: function() {
			_.bindAll(this);
			this.original = this.model;
			this.model = this.model.clone();
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['custo'].converter = moneyConverter;
			bindings['preco'].converter = moneyConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
		},
		onCancel: function(e) {
			this.$el.dialog('close');
			this.close();
		},
		onConfirm: function(e) {
			if (_validate(e.view)) {
				this.original.set(e.model.attributes);
				this.collection.add(this.original);
				this.$el.dialog('close');
				this.close();
			}
		}
	});
	
	var CategoriaProdutosRowView = Marionette.ItemView.extend({
		template: '#categoria_produtos_row_template',
		tagName: 'tr',
		ui: {
			'custo': 'td:nth-child(3)',
			'preco': 'td:nth-child(4)',
			'updateButton': 'button.coi-action-update',
			'deleteButton': 'button.coi-action-delete'
		},
		triggers: {
			'click .coi-action-update': 'update',
			'click .coi-action-delete': 'delete'
		},
		modelEvents: {
			'change': 'render'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.ui.updateButton.button();
			this.ui.deleteButton.button();
		},
		onUpdate: function(e) {
			new CategoriaProdutoView({model: e.model, collection: e.model.collection}).render(); //FIXME
		},
		onDelete: function(e) {
			e.model.collection.remove(e.model);
		}
	});
	
	var CategoriaProdutosView = COI.FormCompositeView.extend({
		template: '#categoria_produtos_template',
		itemViewContainer: 'tbody',
		itemView: CategoriaProdutosRowView,
		initialize: function() {
			_.bindAll(this);
		},
		ui: {
			'produtos': 'table'
		},
		onRender: function() {
			this.ui.produtos.table({
				buttons: {
					'Adicionar': this.doCreate
				}
			}).css('width', '100%');
		},
		doCreate: function(e) {
			e.preventDefault();
			new CategoriaProdutoView({model: new Produto(), collection: this.collection}).render(); //FIXME
		}
	});
	
	var CategoriaComissaoView = Marionette.ItemView.extend({
		template: '#categoria_comissao_template',
		tagName: 'tr',
		ui: {
			'input': 'input[type=text]'
		},
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.$el, 'name');
			bindings['porcentagem'].converter = percentageConverter;
			this.modelBinder().bind(this.model, this.$el, bindings);
		}
	});
	
	var CategoriaComissoesView = COI.FormCompositeView.extend({
		template: '#categoria_comissoes_template',
		itemView: CategoriaComissaoView,
		itemViewContainer: 'table',
		initialize: function() {
			_.bindAll(this);
		}
	});
	
	var CategoriaView = COI.FormView.extend({
		template: '#categoria_template',
		regions: {
			'produtos': '#produtos',
			'comissoes': '#comissoes'
		},
		modelEvents: {
			'change:produtos': 'renderProdutos',
			'change:comissoes': 'renderComissoes'
		},
		initialize: function() {
			_.bindAll(this);
			if (this.model.isNew()) {
				var model = this.model;
				new PartesComissionadas().fetch({
					success: function(partes) {
						var comissoes = [];
						partes.each(function(parte) {
							comissoes.push(new Comissao({
								parte: parte.get('descricao'),
								porcentagem: 0
							}));
						});
						model.get('comissoes').add(comissoes);
					}
				});
			} else {
				this.model.fetch();
			}
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.el);
			
			this.renderProdutos();
			this.renderComissoes();
		},
		renderProdutos: function() {
			this.produtos.show(new CategoriaProdutosView({collection: this.model.get('produtos')}));
		},
		renderComissoes: function() {
			this.comissoes.show(new CategoriaComissoesView({collection: this.model.get('comissoes')}));
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		onCancel: function(e) {
			Backbone.history.navigate('categorias', true);
		},
		onConfirm: function(e) {
			if (_validate(e.view)) {
				if (!e.model.save(null, {wait: true, success: e.view.onComplete, error: e.view.onError})) {
					_notifyWarning(e.model.validationError);
				}
			}
		},
		onComplete: function() {
			Backbone.history.navigate('categorias', true);
			_notifySuccess();
		},
		onError: function() {
			_notifyUpdateFailure();
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('categoria', function() {
			COI.body.show(new CategoriaView({model: new Categoria()}));
		});
		COI.router.route('categoria(/:id)', function(id) {
			COI.body.show(new CategoriaView({model: new Categoria({id: id})}));
		});
	});
});