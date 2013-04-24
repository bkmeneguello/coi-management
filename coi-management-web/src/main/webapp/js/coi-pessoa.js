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
		defaults: {
			nome: null,
			codigo: null,
			partes: new Partes()
		},
		parse: function(resp, options) {
			resp.partes = new Partes(resp.partes);
			return resp;
		}
	});
	
	var PessoaParteView = Marionette.ItemView.extend({
		tagName: 'tr',
		template: '#pessoa_parte_template',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		events: {
			'click .coi-action-remove': 'doRemove'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.$el);
			this.$el.form();
		},
		doRemove: function(e) {
			e.preventDefault();
			this.model.collection.remove(this.model);
		}
	});
	
	var PessoaPartesView = Marionette.CompositeView.extend({
		template: '#pessoa_partes_template',
		itemViewContainer: 'table',
		itemView: PessoaParteView,
		templateHelpers: {
			partes_list: []
		},
		ui: {
			'parte': '#parte'
		},
		events: {
			'click .coi-action-include': 'doInclude'
		},
		initialize: function() {
			_.bindAll(this);
			var that = this;
			new Partes().fetch({
				success: function(partes) {
					that.templateHelpers.partes_list.length = 0;
					partes.each(function(parte) {
						that.templateHelpers.partes_list.push(parte.get('descricao'));
					});
					that.render();
				}
			});
		},
		onRender: function() {
			this.$el.form();
		},
		doInclude: function(e) {
			e.preventDefault();
			this.collection.add(new Parte({descricao: this.ui.parte.val()}));
		}
	});
	
	var PessoaView = Marionette.Layout.extend({
		template: '#pessoa_template',
		tagName: 'form',
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		events: {
			'click .coi-action-confirm': 'doConfirm',
			'click .coi-action-cancel': 'doCancel'
		},
		regions: {
			'partes': '#partes'
		},
		initialize: function() {
			_.bindAll(this);
			this.listenTo(this.model, 'change:partes', this.renderPartes);
			if (this.model.isNew()) {
				this.onNew();
			} else {
				this.model.fetch();
			}
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.el);
			this.$el.form();
			this.renderPartes();
		},
		renderPartes: function() {
			this.partes.show(new PessoaPartesView({collection: this.model.get('partes')}));
		},
		onNew: function() {
			
		},
		doCancel: function(e) {
			e.preventDefault();
			Backbone.history.navigate('pessoas', true);
		},
		doConfirm: function(e) {
			e.preventDefault();
			var that = this;
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: that.onComplete});
			}
		},
		onComplete: function() {
			Backbone.history.navigate('pessoas', true);
			_notifySuccess();
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