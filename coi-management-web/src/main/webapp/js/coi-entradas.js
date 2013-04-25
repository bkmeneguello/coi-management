"use strict";

COI.module("Pessoas", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Entrada = Backbone.Model.extend({
		defaults: {
			data: null,
			paciente: null,
			valor: null,
			tipo: null
		},
		parse: function(resp, options) {
			resp.data = new Date(resp.data);
			return resp;
		}
	});

	var Entradas = Backbone.Collection.extend({
		url: '/rest/entradas',
		model: Entrada
	});
	
	var EntradaRowView = Marionette.ItemView.extend({
		template: '#entrada_row_template',
		tagName: 'tr',
		events: {
			'click .coi-action-update': 'doUpdate',
			'click .coi-action-delete': 'doDelete'
		},
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.$('button').button();
		},
		doUpdate: function(e) {
			e.preventDefault();
			Backbone.history.navigate('entrada/' + this.model.get('id'), true);
		},
		doDelete: function(e) {
			e.preventDefault();
			
			var model = this.model;
			_promptDelete(function() {
				model.destroy({
					wait: true,
					success: function(model, response, options) {
						_notifyDelete();
					},
					error: function(model, xhr, options) {
						_notifyDeleteFailure();
					}
				});
			});
		}
	});
	
	var EntradasView = Marionette.CompositeView.extend({
		template: '#entradas_template',
		tagName: 'form',
		itemViewContainer: 'tbody',
		itemView: EntradaRowView,
		events: {
			'click .coi-action-cancel': 'doCancel',
			'click .coi-action-create': 'doCreate'
		},
		ui: {
			'table': 'table'
		},
		initialize: function() {
			_.bindAll(this);
			this.collection.fetch();
		},
		onRender: function() {
			this.$el.form();
			this.ui.table.table().css('width', '100%');
		},
		doCancel: function(e) {
			e.preventDefault();
			Backbone.history.navigate('', true);
		},
		doCreate: function(e) {
			e.preventDefault();
			Backbone.history.navigate('entrada', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('entradas', function() {
			COI.body.show(new EntradasView({collection: new Entradas()}));
		});
	});
});