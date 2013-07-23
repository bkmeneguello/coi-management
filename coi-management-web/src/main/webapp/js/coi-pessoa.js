"use strict";

COI.module("Pessoa", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Parte = Backbone.Model.extend({
		defaults: {
			descricao: null
		}
	});

	var Partes = Backbone.Collection.extend({
		url: '/rest/partes',
		model: Parte
	});

	var Pessoa = Backbone.Model.extend({
		urlRoot: '/rest/pessoas',
		defaults: function() {
			return {
				nome: null,
				codigo: null,
				partes: new Partes()
			};
		},
		parse: function(resp, options) {
			resp.partes = new Partes(resp.partes, {parse: true});
			return resp;
		}
	});
	
	var PessoaParteView = Marionette.ItemView.extend({
		tagName: 'tr',
		template: '#pessoa_parte_template',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		ui: {
			'removeButton': 'button.coi-action-remove'
		},
		triggers: {
			'click .coi-action-remove': 'remove'
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.$el);
			this.ui.removeButton.button();
		},
		onRemove: function(e) {
			this.model.collection.remove(this.model);
		}
	});
	
	var PessoaPartesView = COI.FormCompositeView.extend({
		template: '#pessoa_partes_template',
		itemViewContainer: 'table',
		itemView: PessoaParteView,
		templateHelpers: {
			partes_list: []
		},
		ui: {
			'parte': 'select',
			'includeButton': '.coi-action-include'
		},
		triggers: {
			'click .coi-action-include': 'include'
		},
		initialize: function() {
			var that = this;
			that.templateHelpers.partes_list.length = 0;
			new Partes().fetch({
				success: function(partes) {
					partes.each(function(parte) {
						that.templateHelpers.partes_list.push(parte.get('descricao'));
					});
					that.render();
				}
			});
		},
		onRender: function() {
			this.ui.parte.input();
			this.ui.includeButton.button();
		},
		onInclude: function(e) {
			this.collection.add(new Parte({descricao: this.ui.parte.val()}));
		}
	});
	
	var PessoaView = COI.FormView.extend({
		template: '#pessoa_template',
		regions: {
			'partes': '#partes'
		},
		modelEvents: {
			'change:partes': 'renderPartes'
		},
		initialize: function() {
			if (!this.model.isNew()) {
				this.model.fetch();
			}
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.el);
			this.renderPartes();
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		renderPartes: function() {
			this.partes.show(new PessoaPartesView({collection: this.model.get('partes')}));
		},
		onCancel: function(e) {
			Backbone.history.navigate('pessoas', true);
		},
		onConfirm: function(e) {
			var that = this;
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: that.onComplete, error: this.onError});
			}
		},
		onComplete: function() {
			Backbone.history.navigate('pessoas', true);
			_notifySuccess();
		},
		onError: function(model, resp, options) {
			_notifyError(resp.responseText);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pessoa', function() {
			COI.body.show(new PessoaView({model: new Pessoa()}));
		});
		COI.router.route('pessoa(/:id)', function(id) {
			COI.body.show(new PessoaView({model: new Pessoa({id: id})}));
		});
	});
});