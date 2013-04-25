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
		defaults: {
			data: new Date(),
			paciente: new Pessoa(),
			valor: null,
			tipo: null,
			produtos: new Produtos()
		},
		parse: function(resp, options) {
			resp.data = new Date(resp.data);
			resp.produtos = new Produtos(resp.produtos);
			return resp;
		}
	});
	
//	var PessoaParteView = Marionette.ItemView.extend({
//		tagName: 'tr',
//		template: '#pessoa_parte_template',
//		modelBinder: function() {
//			return new Backbone.ModelBinder();
//		},
//		events: {
//			'click .coi-action-remove': 'doRemove'
//		},
//		initialize: function() {
//			_.bindAll(this);
//		},
//		onRender: function() {
//			this.modelBinder().bind(this.model, this.$el);
//			this.$el.form();
//		},
//		doRemove: function(e) {
//			e.preventDefault();
//			this.model.collection.remove(this.model);
//		}
//	});
//	
//	var PessoaPartesView = Marionette.CompositeView.extend({
//		template: '#pessoa_partes_template',
//		itemViewContainer: 'table',
//		itemView: PessoaParteView,
//		templateHelpers: {
//			partes_list: []
//		},
//		ui: {
//			'parte': '#parte'
//		},
//		events: {
//			'click .coi-action-include': 'doInclude'
//		},
//		initialize: function() {
//			_.bindAll(this);
//			var that = this;
//			new Partes().fetch({
//				success: function(partes) {
//					that.templateHelpers.partes_list.length = 0;
//					partes.each(function(parte) {
//						that.templateHelpers.partes_list.push(parte.get('descricao'));
//					});
//					that.render();
//				}
//			});
//		},
//		onRender: function() {
//			this.$el.form();
//		},
//		doInclude: function(e) {
//			e.preventDefault();
//			this.collection.add(new Parte({descricao: this.ui.parte.val()}));
//		}
//	});
	
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
//		regions: {
//			'partes': '#partes'
//		},
		ui: {
			'cliente': '#cliente'
		},
		initialize: function() {
			_.bindAll(this);
//			this.listenTo(this.model, 'change:partes', this.renderPartes);
			if (this.model.isNew()) {
				this.onNew();
			} else {
				this.model.fetch();
			}
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['data'].converter = dateConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			this.$el.form();
//			this.renderPartes();
			this.ui.cliente.autocomplete({
				source: '/rest/entradas/clientes',
				minLength: 3,
				appendTo: this.ui.cliente.closest('.coi-form-item'),
				response: function(event, ui) {
					$.each(ui.content, function(index, element) {
						element.label = element.nome;
					});
				},
				select: function(event, ui) {
					console.log(ui);
				}
			});
		},
//		renderPartes: function() {
//			this.partes.show(new PessoaPartesView({collection: this.model.get('partes')}));
//		},
		onNew: function() {
			
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