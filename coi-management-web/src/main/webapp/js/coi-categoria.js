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
		}
	});
	
	var CategoriaProdutoView = Marionette.ItemView.extend({
		template: '#categoria_produto_template',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		initialize: function() {
			_.bindAll(this);
			this.original = this.model;
			this.model = this.model.clone();
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['custo'].converter = autoNumericConverter;
			bindings['preco'].converter = autoNumericConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			var that = this;
			this.$el.form();
			this.$el.dialog({
				title: 'Produto',
				dialogClass: 'no-close',
				height: 450,
				width: 510,
				modal: true,
				buttons: {
					'Cancelar': that.onCancel,
					'Confirmar': that.onConfirm
				}
			});
		},
		onCancel: function(e) {
			e.preventDefault();
			this.$el.dialog('close');
			this.close();
		},
		onConfirm: function(e) {
			e.preventDefault();
			if (_validate(this)) {
				this.original.set(this.model.attributes);
				this.collection.add(this.original);
				this.$el.dialog('close');
				this.close();
			}
		}
	});
	
	var CategoriaProdutosRowView = Marionette.ItemView.extend({
		template: '#categoria_produtos_row_template',
		tagName: 'tr',
		events: {
			'click .coi-action-update': 'onUpdate',
			'click .coi-action-delete': 'onDelete'
		},
		initialize: function() {
			_.bindAll(this);
			this.listenTo(this.model, 'change', this.render);
		},
		onRender: function() {
			this.$el.form();
		},
		onUpdate: function(e) {
			e.preventDefault();
			new CategoriaProdutoView({model: this.model, collection: this.model.collection}).render(); //FIXME
		},
		onDelete: function(e) {
			e.preventDefault();
			this.model.collection.remove(this.model);
		}
	});
	
	var CategoriaProdutosView = Marionette.CompositeView.extend({
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
			this.$el.form();
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
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.$el, 'name');
			bindings['porcentagem'].converter = autoNumericConverter;
			this.modelBinder().bind(this.model, this.$el, bindings);
			this.$el.form();
		}
	});
	
	var CategoriaComissoesView = Marionette.CompositeView.extend({
		template: '#categoria_comissoes_template',
		itemView: CategoriaComissaoView,
		itemViewContainer: 'table',
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.$el.form();
		}
	});
	
	var CategoriaView = Marionette.Layout.extend({
		template: '#categoria_template',
		tagName: 'form',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		events: {
			'click .coi-action-confirm': 'doConfirm',
			'click .coi-action-cancel': 'doCancel'
		},
		regions: {
			'produtos': '#produtos',
			'comissoes': '#comissoes'
		},
		initialize: function() {
			_.bindAll(this);
			this.listenTo(this.model, 'change:produtos', this.renderProdutos);
			this.listenTo(this.model, 'change:comissoes', this.renderComissoes);
			if (this.model.isNew()) {
				this.onNew();
			} else {
				this.model.fetch();
			}
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.el);
			this.$el.form();
			this.renderProdutos();
			this.renderComissoes();
		},
		renderProdutos: function() {
			this.produtos.show(new CategoriaProdutosView({collection: this.model.get('produtos')}));
		},
		renderComissoes: function() {
			this.comissoes.show(new CategoriaComissoesView({collection: this.model.get('comissoes')}));
		},
		onNew: function() {
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
		},
		doCancel: function(e) {
			e.preventDefault();
			Backbone.history.navigate('categorias', true);
		},
		doConfirm: function(e) {
			e.preventDefault();
			var that = this;
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: that.onComplete});
			}
		},
		onComplete: function() {
			Backbone.history.navigate('categorias', true);
			_notifySuccess();
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