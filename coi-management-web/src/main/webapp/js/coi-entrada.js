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
			resp.data = new Date(resp.data);
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
		events: {
			'click .coi-action-remove': 'doRemove'
		},
		ui: {
			'quantidade': 'input'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.$el);
			this.$el.form();
			//this.ui.quantidade.spinner({min: 1}); //TODO
		},
		doRemove: function(e) {
			e.preventDefault();
			this.model.collection.remove(this.model);
		}
	});
	
	var EntradaProdutosView = Marionette.CompositeView.extend({
		template: '#entrada_produtos_template',
		itemViewContainer: 'tbody',
		itemView: EntradaProdutoView,
		ui: {
			'produto': '#produto',
			'table': 'table'
		},
		events: {
			'click .coi-action-include': 'doInclude'
		},
		initialize: function() {
			_.bindAll(this);
			this.model = new Produto();
		},
		onRender: function() {
			this.$el.form();
			this.ui.table.table().css('width', '100%');
			var that = this;
			this.ui.produto.autocomplete({
				source: '/rest/categorias/produtos',
				minLength: 3,
				appendTo: this.ui.produto.closest('.coi-form-item'),
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
		doInclude: function(e) {
			e.preventDefault();
			this.collection.add(this.model);
			this.model = new Produto();
		}
	});
	
	var EntradaView = Marionette.Layout.extend({
		template: '#entrada_template',
		tagName: 'form',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		events: {
			'click .coi-action-confirm': 'doConfirm',
			'click .coi-action-cancel': 'doCancel'
		},
		regions: {
			'produtos': '#produtos'
		},
		ui: {
			'paciente': '#paciente'
		},
		templateHelpers: {
			tipos_list: []
		},
		initialize: function() {
			_.bindAll(this);
			this.listenTo(this.model, 'change:produtos', this.renderProdutos);
			if (this.model.isNew()) {
				this.onNew();
			} else {
				this.model.fetch({
					success: this.onFetch
				});
			}
			var that = this;
			that.templateHelpers.tipos_list.length = 0;
			$.get('/rest/entradas/meios', function(meios) {
				$.each(meios, function(index, value) {
					that.templateHelpers.tipos_list.push(value);
				});
				that.render();
				that.$el.form();
			});
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['data'].converter = dateConverter;
			bindings['valor'].converter = autoNumericConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			this.$el.form();
			this.renderProdutos();
			var model = this.model;
			this.ui.paciente.autocomplete({
				source: '/rest/pessoas',
				minLength: 3,
				appendTo: this.ui.paciente.closest('.coi-form-item'),
				response: function(event, ui) {
					$.each(ui.content, function(index, element) {
						element.label = '[' + element.codigo + '] ' + element.nome;
						element.value = element.nome;
					});
				},
				select: function(event, ui) {
					var element = ui.item;
					model.set('paciente', new Pessoa(_.omit(element, 'label', 'value')));
				}
			})
			.change(function() {
				if ($(this).val() != model.get('paciente').get('nome')) {
					$(this).val(null);
				}
			})
			.blur(function() {
				if ($.isBlank($(this).val())) {
					model.get('paciente').clear();
				} else if (model.get('paciente').isNew()) {
					$(this).val(null).blur();
				}
			})
			.autocomplete('widget')
			.css('z-index', 100);
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		renderProdutos: function() {
			this.produtos.show(new EntradaProdutosView({collection: this.model.get('produtos')}));
		},
		onNew: function() {
			
		},
		onFetch: function() {
			this.ui.paciente.val(this.model.get('paciente').get('nome'));
			this.$el.form();
		},
		doCancel: function(e) {
			e.preventDefault();
			Backbone.history.navigate('entradas', true);
		},
		doConfirm: function(e) {
			e.preventDefault();
			var that = this;
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: that.onComplete});
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