"use strict";

COI.module("Categorias", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Categoria = Backbone.Model.extend({
		defaults: {
			descricao: null
		}
	});

	var Categorias = Backbone.Paginator.requestPager.extend({
		url: 'rest/categorias',
		model: Categoria,
		paginator_core: {
			type: 'GET',
			dataType: 'json',
			url: 'rest/categorias'
		},
		paginator_ui: {
			firstPage: 0,
			currentPage: 0
		},
		server_api: {
			'page' : function() {
				return this.currentPage;
			}
		}
	});
	
	var CategoriaRowView = COI.ActionRowView.extend({
		template: '#categoria_row_template',
		initialize: function() {
			_.bindAll(this);
		},
		onUpdate: function(e) {
			Backbone.history.navigate('categoria/' + e.model.get('id'), true);
		},
		onDelete: function(e) {
			_promptDelete(function() {
				e.model.destroy({
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
	
	var CategoriasView = COI.GridView.extend({
		searchView: COI.SimplePageView,
		itemView: CategoriaRowView,
		templateHelpers: {
			header: 'Categorias',
			columns: {
				descricao: 'Descrição'
			}
		},
		initialize: function() {
			this.collection.fetch();
		},
		onCancel: function(e) {
			Backbone.history.navigate('', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('categoria', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('categorias', function() {
			COI.body.show(new CategoriasView({collection: new Categorias()}));
		});
	});
});