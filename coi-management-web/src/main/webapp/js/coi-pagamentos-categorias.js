"use strict";

COI.module("PagamentosCategorias", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Categoria = Backbone.Model.extend({
		urlRoot: 'rest/pagamentos/categorias',
		defaults: {
			descricao: null
		}
	});
	
	var Categorias = Backbone.Paginator.requestPager.extend({
		url: 'rest/pagamentos/categorias',
		model: Categoria,
		paginator_core: {
			type: 'GET',
			dataType: 'json',
			url: 'rest/pagamentos/categorias'
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
	
	var RowView = COI.ActionRowView.extend({
		template: '#pagamento_categoria_row_template',
		onUpdate: function(e) {
			Backbone.history.navigate('pagamento-categoria/' + this.model.get('id'), true);
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
	
	var View = COI.GridView.extend({
		searchView: COI.SimplePageView,
		itemView: RowView,
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
			Backbone.history.navigate('pagamentos', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('pagamento-categoria', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pagamento-categorias', function() {
			COI.body.show(new View({collection: new Categorias()}));
		});
	});
});