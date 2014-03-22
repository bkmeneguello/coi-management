"use strict";

COI.module("Categoria", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Custo = Backbone.Model.extend({
		defaults: {
			dataInicioVigencia: null,
			dataFimVigencia: null,
			custo: 0
		},
		toJSON: function(options) {
			var attributes = _.clone(this.attributes);
			attributes.dataInicioVigencia = toTimestamp(attributes.dataInicioVigencia);
			attributes.dataFimVigencia = toTimestamp(attributes.dataFimVigencia);
			return attributes;
		}
	});

	var Custos = Backbone.Collection.extend({
		model: Custo
	});
	
	var Produto = Backbone.Model.extend({
		defaults: function() {
			return {
				codigo: null,
				descricao: null,
				custos: new Custos(),
				custo: null,
				preco: null,
				estocavel: false
			};
		},
		parse: function(resp, options) {
			resp.custos = new Custos(resp.custos, {parse: true});
			return resp;
		}
	});

	var Produtos = Backbone.Collection.extend({
		model: Produto
	});

	var Comissao = Backbone.Model.extend({
		defaults: {
			parte: null,
			porcentagem: null
		}
	});

	var Comissoes = Backbone.Collection.extend({
		model: Comissao
	});
	
	var Categoria = Backbone.Model.extend({
		urlRoot: 'rest/categorias',
		defaults: function() {
			return {
				descricao: null,
				produtos: new Produtos(),
				comissoes: new Comissoes()
			};
		},
		parse: function(resp, options) {
			resp.produtos = new Produtos(resp.produtos, {parse: true});
			resp.comissoes = new Comissoes(resp.comissoes, {parse: true});
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
	
	var CustoView = Marionette.ItemView.extend({
		template: '#categoria_produto_custo_template',
		tagName: 'tr',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		triggers: {
			'click .coi-action-delete': 'remover'
		},
		ui: {
			'dataInicioVigencia': 'input[name=dataInicioVigencia]',
			'dataFimVigencia': 'input[name=dataFimVigencia]',
			'custo': 'input[name=custo]',
			'removerButton': 'button.coi-action-delete'
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['dataInicioVigencia'].converter = dateConverter;
			bindings['dataFimVigencia'].converter = dateConverter;
			bindings['custo'].converter = moneyConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.ui.dataInicioVigencia.input();
			this.ui.dataFimVigencia.input();
			this.ui.custo.input();
			this.ui.removerButton.button();
		},
		onRemover: function(e) {
			e.model.collection.remove(e.model);
		}
	});
	
	var CustosView =  Marionette.CompositeView.extend({
		template: '#categoria_produto_custos_template',
		itemView: CustoView,
		itemViewContainer: 'tbody',
		ui: {
			'includeCusto': '.coi-action-include'
		},
		triggers: {
			'click .coi-action-include': 'adicionarCusto'
		},
		onRender: function() {
			this.ui.includeCusto.button();
		},
		onAdicionarCusto: function() {
			this.collection.add(new Custo());
		}
	});
	
	var CategoriaProdutoView = COI.PopupFormView.extend({
		template: '#categoria_produto_template',
		header: 'Produto',
		width: 550,
		height: 500,
		regions: {
			'custos': '#custos'
		},
		modelEvents: {
			'change:custos': 'renderCustos'
		},
		initialize: function() {
			_.bindAll(this);
			this.original = this.model;
			this.model = this.model.clone();
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['preco'].converter = moneyConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			this.renderCustos();
		},
		onCancel: function(e) {
			this.$el.dialog('close');
			this.close();
		},
		onConfirm: function(e) {
			if (_validate(e.view)) {
				this.original.set(e.model.attributes);
				var custos = this.original.get('custos');
				for (var i = 0; i < custos.length; i++) {
					var now = new Date();
					var custo = custos.at(i);
					if (custo.get('dataInicioVigencia') <= now.getTime() && (custo.get('dataFimVigencia') == null || custo.get('dataFimVigencia') >= now.getTime())) {
						this.original.set('custo', custo.get('custo'));
					}
				}
				this.collection.add(this.original);
				this.$el.dialog('close');
				this.close();
			}
		},
		renderCustos: function() {
			this.custos.show(new CustosView({collection: this.model.get('custos')}));
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
					'Adicionar': this.onCreate
				}
			}).css('width', '100%');
		},
		onCreate: function(e) {
			e.preventDefault(); //FIXME
			new CategoriaProdutoView({model: new Produto(), collection: this.collection}).render(); //FIXME
		}
	});
	
	var CategoriaComissaoView = Marionette.ItemView.extend({
		template: '#categoria_comissao_template',
		tagName: 'tr',
		ui: {
			'input': 'input[type=text]',
			'actionRemove': 'button'
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
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.$el, 'name');
			bindings['porcentagem'].converter = percentageConverter;
			this.modelBinder().bind(this.model, this.$el, bindings);
			
			this.ui.input.input();
			this.ui.actionRemove.button();
		},
		onRemove: function(e) {
			this.model.collection.remove(this.model);
		}
	});
	
	var CategoriaComissoesView = COI.FormCompositeView.extend({
		template: '#categoria_comissoes_template',
		itemView: CategoriaComissaoView,
		itemViewContainer: 'table',
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
				source: 'rest/partes/comissionadas',
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
			this.collection.add(new Comissao({
				parte: this.ui.parte.val()
			}));
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
			if (!this.model.isNew()) {
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