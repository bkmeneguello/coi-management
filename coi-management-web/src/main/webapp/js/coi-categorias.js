"use strict";

COI.module("Categorias", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Categoria = Backbone.Model.extend({
		defaults: {
			descricao: null
		}
	});

	var Categorias = Backbone.Collection.extend({
		url: '/rest/categorias',
		model: Categoria
	});
	
	var CategoriaRowView = Marionette.ItemView.extend({
		template: '#categoria_row_template',
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
			Backbone.history.navigate('categoria/' + this.model.get('id'), true);
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
	
	var CategoriasView = Marionette.CompositeView.extend({
		template: '#categorias_template',
		tagName: 'form',
		itemViewContainer: 'tbody',
		itemView: CategoriaRowView,
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
			Backbone.history.navigate('categoria', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('categorias', function() {
			COI.body.show(new CategoriasView({collection: new Categorias()}));
		});
	});
});